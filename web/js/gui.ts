var $;
var Ajax;
var swal;

class E {
    public static $PAGE_LIST = $('#page_list');
    public static $NEIGHBORS = $('#neighbors');
    public static $MODULES = $('#ixis');
}

class Page {
    private identifier : String;
    private name : String;
    private $li;
    private $page;

    public static NEIGHBORS : Page = new Page("Neighbors");
    public static CONFIG : Page = new Page("Config");
    public static LOG : Page = new Page("Log");
    public static IXIS : Page = new Page("IXI Modules");

    private static current_page : Page = Page.NEIGHBORS;

    private constructor(name : String) {
        this.name = name;
        this.identifier = name.toLowerCase().replace(/ /g, "_");
        this.$li = $('<li>').text(this.name.toUpperCase());
        const thisPage = this;
        this.$li.click(function(event) { Page.switch_to(thisPage); });
        this.$page = $('#page_'+this.identifier);
    }

    public static init_pages() {
        E.$PAGE_LIST.html("");
        this.NEIGHBORS.init();
        this.CONFIG.init();
        this.LOG.init();
        this.IXIS.init();
    }

    private init() {
        E.$PAGE_LIST.append(this.$li);
        this.$page.addClass("hidden").prepend($("<header>").html($("<h1>").text(this.name)));
    }

    public static switch_to(page : Page) : void {

        Page.current_page.$page.addClass("hidden");
        Page.current_page.$li.removeClass("highlighted");

        page.$page.removeClass("hidden");
        page.$li.addClass("highlighted");

        Page.current_page = page;
    }
}


class Form {

    public static save_config() : void {
        alert("x");
    }

    public static load_config() : void {
        Ajax.INSTANCE.get_config(function (config : Object) {
            Object.keys(config).forEach(function (property : string) {
                $('#config_'+property).val(config[property]);
            });
        });
    }
}

class NeighborViewer {

    public static load() : void {
        Ajax.INSTANCE.get_neighbor_stats(NeighborViewer.printNeighborStats);
    }

    private static printNeighborStats(neighbors) : void {
        E.$NEIGHBORS.html("");

        for (let i = 0; i < neighbors.length; i++){

            const neighbor = neighbors[i];
            const $stats = $("<div>").addClass("stats")
                .append($("<div>").addClass("all").text(neighbor['all']))
                .append($("<div>").addClass("new").text(neighbor['new']))
                .append($("<div>").addClass("requested").text(neighbor['requested']))
                .append($("<div>").addClass("invalid").text(neighbor['invalid']))
                .append($("<div>").addClass("ignored").text(neighbor['ignored']));
            const $neighbor = $("<div>").addClass("neighbor")
                .append($("<div>").addClass("name").text(neighbor['address']))
                .append($stats)
                .append($("<button>").text("remove").click(function () {
                    Ajax.INSTANCE.remove_neighbor(neighbor['address']);
                }));
            E.$NEIGHBORS.append($neighbor);
        };
    }

    public static open_add_neighbor_dialog() : void {
        swal("Enter Neighbor Address", "Format: `IP:PORT` or `HOST:PORT`\n\nExample: `123.4.5.678:1337` or `example.org:1337`", {
            button: "add neighbor",
            content: "input",
        }).then((value) => {
            if(value.length > 0)
                Ajax.INSTANCE.add_neighbor(value);
        });
    }
}

class ModuleViewer {

    public static load() : void {
        Ajax.INSTANCE.get_modules(ModuleViewer.print_modules);
    }

    private static print_modules(modules) : void {
        E.$MODULES.html("");

        for (let i = 0; i < modules.length; i++){

            const module = modules[i];

            const $module = $("<div>").addClass("ixi")
                .append($("<div>").addClass("name").text(module['name'] ? module['name'] : module['path']));
            if(module['web_gui'])
                $module.append($("<button>").text("open"))
            if(module['repository'])
                $module.append($("<button>").text("check updates"))
            $module
                .append($("<button>").text("config"))
                .append($("<button>").text("uninstall").click(function () {
                    Ajax.INSTANCE.uninstall_module(module['path'], );
                }));
            E.$MODULES.append($module);
        };
    }

    public static open_install_module_dialog() : void {
        swal("Enter Github Repository", "Format: `username/repository` or Github URL\n\nExample: `iotaledger/chat.ixi`", {
            button: "install module",
            content: "input",
        }).then((value) => {
            value = value.replace(/^(https:\/\/)?(github.com\/)/g, "");
            if(value.split("/").length != 2)
                return logError("'"+value+"' is does not match the required format 'username/repository'.", "Unexpected Format Repository");
            if(value.length > 0)
                Ajax.INSTANCE.install_module(value);
        });
    }
}

function logError(message : string, title : string = "Whoops") {
    swal(title, message, "error");
}

function logSuccess(message : string, title : string = "Success") {
    swal(title, message, "success");
}