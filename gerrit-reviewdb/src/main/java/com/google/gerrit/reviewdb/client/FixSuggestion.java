// Copyright (C) 2016 The Android Open Source Project
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

package com.google.gerrit.reviewdb.client;

import java.util.List;

public class FixSuggestion {
  public String fixId;
  public String description;
  public List<FixReplacement> replacements;

  public FixSuggestion(String fixId, String description,
      List<FixReplacement> replacements) {
    this.fixId = fixId;
    this.description = description;
    this.replacements = replacements;
  }

  @Override
  public String toString() {
    return "FixSuggestion{" +
        "fixId='" + fixId + '\'' +
        ", description='" + description + '\'' +
        ", replacements=" + replacements +
        '}';
  }
}
