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
(function() {
  'use strict';

  Polymer({
    is: 'gr-dropdown',

    properties: {
      items: Array,
      topContent: Object,
      horizontalAlign: {
        type: String,
        value: 'left',
      },
      _hasAvatars: String,
    },

    attached: function() {
      this.$.restAPI.getConfig().then(function(cfg) {
        this._hasAvatars = !!(cfg && cfg.plugin && cfg.plugin.has_avatars);
      }.bind(this));
    },

    _handleDropdownTap: function(e) {
      this.$.dropdown.close();
    },

    _showDropdownTapHandler: function(e) {
      this.$.dropdown.open();
    },

    _getClassIfBold: function(bold) {
      return bold ? 'bold-text' : '';
    },

    _computeURLHelper: function(host, path) {
      return '//' + host + path;
    },

    _computeRelativeURL: function(path) {
      var host = window.location.host;
      return this._computeURLHelper(host, path);
    },
  });
})();
