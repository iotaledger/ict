var $;
var ModuleViewer;
var NeighborViewer;
var logError;
var logSuccess;
var default_config;
var swal;

class Ajax {

    public static INSTANCE : Ajax = new Ajax(window.location.protocol + "//" + window.location.host);

    private password : string = Ajax.get_cookie("password");
    private base_url : string;

    public constructor(base_url : string) {
        this.base_url = base_url;
    }

    public submit(path : string, data : Object, success : (data) => void = function (data) {}, error : (err) => void = logError) : void {

        data['password'] = this.password;
        const name_value_array : Array<Object> = Ajax.json_to_name_value_array(data);
        const $this = this;

        $.ajax({
            dataType: "json",
            method: 'POST',
            data: name_value_array,
            url: this.base_url + path,
            success: function (data) { if (data['error']) error(data['error']); else success(data); },
            error: function (err) {
                if(err['status'] === 401)
                    $this.ask_for_password_and_resubmit(err, path, data, success, error);
                else {
                    console.log(err);
                    error(JSON.stringify(err));
                }
            }
        });
    }

    private ask_for_password_and_resubmit(err401 : Object, path : string, data : Object, success : (data) => void = function (data) {}, error : (err) => void = logError) : void {

        swal({
            title: "Enter Password",
            text: "The password is set in your ict.cfg. The default value is 'change_me_now'.\n\nERROR 401 (" + JSON.stringify(err401['responseText'])+")",
            content: "input"
        }).then(value => {
            this.password = value;
            Ajax.set_cookie("password", value);
            this.submit(path, data, success, error);
        });
    }

    private static set_cookie(name, value) : void {
        let date = new Date();
        date.setTime(date.getTime() + 7*24*60*60*1000);
        const time = "expires="+ date.toUTCString();
        document.cookie = name + "=" + value + ";" + time + ";";
    }

    private static get_cookie(name) : string {
        name = name + "=";
        const decoded_cookie = decodeURIComponent(document.cookie);
        const parts = decoded_cookie.split(';');
        for(let i = 0; i < parts.length; i++) {
            const part = parts[i].trim();
            if (part.indexOf(name) === 0)
                return part.substring(name.length, part.length);
        }
        return "";
    }

    private static json_to_name_value_array(json : Object) : Array<Object> {
        const array : Array<Object> = [];
            Object.keys(json).forEach(function(key) {
                array.push({name: key, value: json[key]});
            });
        return array;
    }

    /* === GENERAL === */

    public get_info(success : (config : Object) => void) : void {
        this.submit("/getInfo", {}, function (data) {
            default_config = data['default_config'];
            success(data);
        });
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

    public get_module_config(path : string, success : (config : Object) => void) : void {
        this.submit("/getModuleConfig", {"path": path}, success, logError);
    }

    public set_module_config(path : string, config : Object) : void {
        this.submit("/setModuleConfig", {"path": path, "config": config}, () => {logSuccess("changes have been applied");}, logError);
    }
}