var $;
var ModuleViewer;
var NeighborViewer;
var logError;
var logSuccess;
var default_config;
var swal;
var Ajax = /** @class */ (function () {
    function Ajax(base_url) {
        this.password = Ajax.get_cookie("password");
        this.base_url = base_url;
    }
    Ajax.prototype.submit = function (path, data, success, error) {
        if (success === void 0) { success = function (data) { }; }
        if (error === void 0) { error = logError; }
        data['password'] = this.password;
        var name_value_array = Ajax.json_to_name_value_array(data);
        var $this = this;
        $.ajax({
            dataType: "json",
            method: 'POST',
            data: name_value_array,
            url: this.base_url + path,
            success: function (data) { if (data['error'])
                error(data['error']);
            else
                success(data); },
            error: function (err) {
                if (err['status'] === 401)
                    $this.ask_for_password_and_resubmit(err, path, data, success, error);
                else {
                    console.log(err);
                    error(JSON.stringify(err));
                }
            }
        });
    };
    Ajax.prototype.ask_for_password_and_resubmit = function (err401, path, data, success, error) {
        var _this = this;
        if (success === void 0) { success = function (data) { }; }
        if (error === void 0) { error = logError; }
        swal({
            title: "Enter Password",
            text: "The password is set in your ict.cfg. The default value is 'change_me_now'.\n\nERROR 401 (" + JSON.stringify(err401['responseText']) + ")",
            content: "input"
        }).then(function (value) {
            _this.password = value;
            Ajax.set_cookie("password", value);
            _this.submit(path, data, success, error);
        });
    };
    Ajax.set_cookie = function (name, value) {
        var date = new Date();
        date.setTime(date.getTime() + 7 * 24 * 60 * 60 * 1000);
        var time = "expires=" + date.toUTCString();
        document.cookie = name + "=" + value + ";" + time + ";";
    };
    Ajax.get_cookie = function (name) {
        name = name + "=";
        var decoded_cookie = decodeURIComponent(document.cookie);
        var parts = decoded_cookie.split(';');
        for (var i = 0; i < parts.length; i++) {
            var part = parts[i].trim();
            if (part.indexOf(name) === 0)
                return part.substring(name.length, part.length);
        }
        return "";
    };
    Ajax.json_to_name_value_array = function (json) {
        var array = [];
        Object.keys(json).forEach(function (key) {
            array.push({ name: key, value: json[key] });
        });
        return array;
    };
    /* === GENERAL === */
    Ajax.prototype.get_info = function (success) {
        this.submit("/getInfo", {}, function (data) {
            default_config = data['default_config'];
            success(data);
        });
    };
    /* === CONFIG === */
    Ajax.prototype.get_config = function (success) {
        this.submit("/getConfig", {}, success);
    };
    Ajax.prototype.set_config = function (config, success) {
        this.submit("/setConfig", { "config": JSON.stringify(config) }, success);
    };
    /* === NEIGHBORS === */
    Ajax.prototype.get_neighbor_stats = function (success) {
        if (success === void 0) { success = function (data) { }; }
        this.submit("/getNeighbors", {}, success);
    };
    Ajax.prototype.add_neighbor = function (address, success) {
        if (success === void 0) { success = function (data) { }; }
        this.submit("/addNeighbor", { "address": address }, NeighborViewer.load, logError);
    };
    Ajax.prototype.remove_neighbor = function (address) {
        this.submit("/removeNeighbor", { "address": address }, NeighborViewer.load, logError);
    };
    /* === MODULES === */
    Ajax.prototype.get_modules = function (success) {
        if (success === void 0) { success = function (data) { }; }
        this.submit("/getModules", {}, success);
    };
    Ajax.prototype.install_module = function (username_slash_repo) {
        this.submit("/addModule", { "user_slash_repo": username_slash_repo }, function (data) {
            logSuccess("`" + username_slash_repo + "` has been installed successfully!", "Installation Complete");
            ModuleViewer.load();
        }, logError);
    };
    Ajax.prototype.uninstall_module = function (path) {
        this.submit("/removeModule", { "path": path }, function (data) {
            logSuccess("`" + path + "` has been uninstalled successfully!", "IXI Module Removed");
            ModuleViewer.load();
        }, logError);
    };
    Ajax.prototype.get_module_config = function (path, success) {
        this.submit("/getModuleConfig", { "path": path }, success, logError);
    };
    Ajax.prototype.set_module_config = function (path, config) {
        this.submit("/setModuleConfig", { "path": path, "config": config }, function () { logSuccess("changes have been applied"); }, logError);
    };
    Ajax.INSTANCE = new Ajax(window.location.protocol + "//" + window.location.host);
    return Ajax;
}());
