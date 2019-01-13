var $;
var ModuleViewer;
var NeighborViewer;
var logError;
var logSuccess;

class Ajax {

    public static INSTANCE : Ajax = new Ajax(window.location.protocol + "//" + window.location.host);

    private base_url : string;

    public constructor(base_url : string) {
        this.base_url = base_url;
    }

    public submit(path : string, data : Object, success : (data) => void = function (data) {}, error : (err) => void = logError) : void {

        data['password'] = 'password'; // TODO replace placeholder, make configurable
        const name_value_array : Array<Object> = Ajax.json_to_name_value_array(data);

        $.ajax({
            dataType: "json",
            method: 'POST',
            data: name_value_array,
            url: this.base_url + path,
            success: function (data) { if (data['error']) error(data['error']); else success(data); },
            error: function (err) { error(JSON.stringify(err)); }
        });
    }

    private static json_to_name_value_array(json : Object) : Array<Object> {
        const array : Array<Object> = [];
            Object.keys(json).forEach(function(key) {
                array.push({name: key, value: json[key]});
            });
        return array;
    }

    /* === CONFIG === */

    public get_config(success : (config : Object) => void) : void {
        this.submit("/getConfig", {}, success);
    }

    public set_config(config, success : (data : Object) => void) : void {
        this.submit("/setConfig", {"config": JSON.stringify(config)}, success);
    }

    /* === NEIGHBORS === */

    public get_neighbor_stats(success : (all_stats : Object) => void = function (data) {}) : void {
        this.submit("/getNeighbors", {}, success);
    }

    public add_neighbor(address : string, success : (all_stats : Object) => void = function (data) {}) : void {
        this.submit("/addNeighbor", {"address": address}, NeighborViewer.load, logError);
    }

    public remove_neighbor(address : string) : void {
        this.submit("/removeNeighbor", {"address": address}, NeighborViewer.load, logError);
    }

    /* === MODULES === */

    public get_modules(success : (modules : Object) => void = function (data) {}) : void {
        this.submit("/getModules", {}, success);
    }

    public install_module(username_slash_repo : string) : void {
        this.submit("/addModule", {"user_slash_repo": username_slash_repo}, function (data) {
            logSuccess("`" + username_slash_repo + "` has been installed successfully!", "Installation Complete");
            ModuleViewer.load();
        }, logError);
    }

    public uninstall_module(path : string) : void {
        this.submit("/removeModule", {"path": path}, function (data) {
            logSuccess("`" + path + "` has been uninstalled successfully!", "IXI Module Removed");
            ModuleViewer.load();
        }, logError);
    }
}