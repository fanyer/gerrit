// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server;

import com.google.gerrit.client.reviewdb.Account;
import com.google.gerrit.client.reviewdb.AccountGroup;
import com.google.gerrit.client.reviewdb.Change;
import com.google.gerrit.client.reviewdb.ReviewDb;
import com.google.gerrit.client.reviewdb.StarredChange;
import com.google.gerrit.client.reviewdb.SystemConfig;
import com.google.gerrit.server.account.AccountCache2;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.config.Nullable;
import com.google.gwtorm.client.OrmException;
import com.google.inject.Inject;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spearce.jgit.lib.PersonIdent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** An authenticated user. */
public class IdentifiedUser extends CurrentUser {
  /** Create an IdentifiedUser, ignoring any per-request state. */
  @Singleton
  public static class GenericFactory {
    private final SystemConfig systemConfig;
    private final AccountCache2 accountCache;

    @Inject
    GenericFactory(final SystemConfig systemConfig,
        final AccountCache2 accountCache) {
      this.systemConfig = systemConfig;
      this.accountCache = accountCache;
    }

    public IdentifiedUser create(final Account.Id id) {
      return new IdentifiedUser(systemConfig, accountCache, null, null, id);
    }
  }

  /**
   * Create an IdentifiedUser, relying on current request state.
   * <p>
   * Can only be used from within a module that has defined request scoped
   * {@code @RemotePeer SocketAddress} and {@code ReviewDb} providers.
   */
  @Singleton
  public static class RequestFactory {
    private final SystemConfig systemConfig;
    private final AccountCache2 accountCache;
    private final Provider<SocketAddress> remotePeerProvider;
    private final Provider<ReviewDb> dbProvider;

    @Inject
    RequestFactory(final SystemConfig systemConfig,
        final AccountCache2 accountCache,
        final @RemotePeer Provider<SocketAddress> remotePeerProvider,
        final Provider<ReviewDb> dbProvider) {
      this.systemConfig = systemConfig;
      this.accountCache = accountCache;
      this.remotePeerProvider = remotePeerProvider;
      this.dbProvider = dbProvider;
    }

    public IdentifiedUser create(final Account.Id id) {
      return new IdentifiedUser(systemConfig, accountCache, remotePeerProvider,
          dbProvider, id);
    }
  }

  private static final Logger log =
      LoggerFactory.getLogger(IdentifiedUser.class);

  private final AccountCache2 accountCache;
  @Nullable
  private final Provider<SocketAddress> remotePeerProvider;
  @Nullable
  private final Provider<ReviewDb> dbProvider;
  private final Account.Id accountId;

  private AccountState state;
  private Set<Change.Id> starredChanges;

  private IdentifiedUser(final SystemConfig systemConfig,
      final AccountCache2 accountCache,
      @Nullable final Provider<SocketAddress> remotePeerProvider,
      @Nullable final Provider<ReviewDb> dbProvider, final Account.Id id) {
    super(systemConfig);
    this.accountCache = accountCache;
    this.remotePeerProvider = remotePeerProvider;
    this.dbProvider = dbProvider;
    this.accountId = id;
  }

  private AccountState state() {
    if (state == null) {
      state = accountCache.get(getAccountId());
    }
    return state;
  }

  /** The account identity for the user. */
  public Account.Id getAccountId() {
    return accountId;
  }

  public Account getAccount() {
    return state().getAccount();
  }

  @Override
  public Set<AccountGroup.Id> getEffectiveGroups() {
    return state().getEffectiveGroups();
  }

  @Override
  public Set<Change.Id> getStarredChanges() {
    if (starredChanges == null) {
      if (dbProvider == null) {
        throw new OutOfScopeException("Not in request scoped user");
      }
      final Set<Change.Id> h = new HashSet<Change.Id>();
      try {
        for (final StarredChange sc : dbProvider.get().starredChanges()
            .byAccount(getAccountId())) {
          h.add(sc.getChangeId());
        }
      } catch (ProvisionException e) {
        log.warn("Cannot query starred by user changes", e);
      } catch (OrmException e) {
        log.warn("Cannot query starred by user changes", e);
      }
      starredChanges = Collections.unmodifiableSet(h);
    }
    return starredChanges;
  }

  public PersonIdent toPersonIdent() {
    final Account ua = getAccount();
    String name = ua.getFullName();
    if (name == null) {
      name = ua.getPreferredEmail();
    }
    if (name == null) {
      name = "Anonymous Coward";
    }

    final String userId = "account-" + ua.getId().toString();
    final String user;
    if (ua.getSshUserName() != null) {
      user = ua.getSshUserName() + "|" + userId;
    } else {
      user = userId;
    }

    String host = null;
    final SocketAddress remotePeer =
        remotePeerProvider != null ? remotePeerProvider.get() : null;
    if (remotePeer instanceof InetSocketAddress) {
      final InetSocketAddress sa = (InetSocketAddress) remotePeer;
      final InetAddress in = sa.getAddress();
      if (in != null) {
        host = in.getCanonicalHostName();
      } else {
        host = sa.getHostName();
      }
    }
    if (host == null) {
      host = "unknown";
    }

    return new PersonIdent(name, user + "@" + host);
  }

  @Override
  public String toString() {
    return "IdentifiedUser[account " + getAccountId() + "]";
  }
}
