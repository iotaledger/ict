var initialized = false;

requirejs.config({
    baseUrl: 'js',
    paths: {
        app: '../js'
    }
});

window.onload = function () {
    if(initialized)
        return;
    initialized = true;
    const deps = ['../node_modules/jquery/dist/jquery','../node_modules/sweetalert/dist/sweetalert.min'];
    requirejs(deps, function () {
        requirejs(['gui', 'ajax'], init)
    });
};

setTimeout(window.onload, 1);

function init() {
    Page.init_pages();
    Page.switch_to(Page.IXIS);
    Form.load_config();
    NeighborViewer.load();
    ModuleViewer.load();
}