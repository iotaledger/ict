var $;
var ModuleViewer;
var NeighborViewer;
var logError;
var logSuccess;
var Ajax = /** @class */ (function () {
    function Ajax(base_url) {
        this.base_url = "http://localhost:4567";
        this.base_url = base_url;
    }
    Ajax.prototype.submit = function (path, data, success, error) {
        if (success === void 0) { success = function (data) { }; }
        if (error === void 0) { error = logError; }
        data['password'] = 'password'; // TODO replace placeholder, make configurable
        var name_value_array = Ajax.json_to_name_value_array(data);
        $.ajax({
            dataType: "json",
            method: 'POST',
            data: name_value_array,
            url: this.base_url + path,
            success: function (data) { if (data['error'])
                error(data['error']);
            else
                success(data); },
            error: error
        });
    };
    Ajax.json_to_name_value_array = function (json) {
        var array = [];
        Object.keys(json).forEach(function (key) {
            array.push({ name: key, value: json[key] });
        });
        return array;
    };
    /* === CONFIG === */
    Ajax.prototype.get_config = function (success) {
        this.submit("/getConfig", {}, success);
    };
    /* === NEIGHBORS === */
    Ajax.prototype.get_neighbor_stats = function (success) {
        if (success === void 0) { success = function (data) { }; }
        this.submit("/getNeighborStats", {}, success);
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
    Ajax.INSTANCE = new Ajax("http://localhost:4567");
    return Ajax;
}());
