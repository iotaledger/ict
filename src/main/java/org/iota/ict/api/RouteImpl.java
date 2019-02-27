package org.iota.ict.api;

import org.apache.logging.log4j.core.LogEvent;
import org.iota.ict.utils.LogAppender;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;

public abstract class RouteImpl implements Route {

    protected String path;
    protected JsonIct jsonIct;

    protected RouteImpl(JsonIct jsonIct, String path) {
        this.jsonIct = jsonIct;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            return execute(request).put("success", true);
        } catch (Throwable t) {
            t.printStackTrace();
            return error(t);
        }
    }

    public abstract JSONObject execute(Request request) throws Throwable;

    protected static JSONObject error(Throwable t) {
        return error(t.getMessage());
    }

    protected static JSONObject error(String message) {
        return new JSONObject().put("error", message).put("success", false);
    }
}
/**
 * @apiDefine basicSuccess
 * @apiSuccess {Object} success `true` if the action was successful, `false` if there was an error.
 * @apiSuccessExample {json} 200 Success
 *     {
 *      "success": true
 *     }
 */

 /**
 * @apiDefine neighborParam
 * @apiParam {String} address Address of the neighbor node to remove. Addresses must be in the following format: `HOST:PORT` (for example, `example.org:1337`).
 */

/**
 * @apiDefine modulePathParam
 * @apiParam {String} path Relative path of the module in the `modules/` directory.
 */

 /**
  * @apiDefine moduleNotFoundErrorExample
  * @apiErrorExample {json} ModuleNotFound Error
  * {
  * "success": false,
  * "error": "No module 'chat.ixi.jar' installed."
  * }
  */

/**
 * @api {post} /getInfo/ getInfo
 * @apiDescription  Get general information about a node, including whether updates are available, the current version of the Ict that it's running, and the default configuration settings.
 * @apiName getInfo
 * @apiExample {curl} Curl
 * curl http://localhost:2187/getInfo \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var password = "password=change_me_now";
 *                
 * var options = {
 * url: 'http://localhost:2187/getInfo',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: password
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * password = 'password=change_me_now'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/getInfo', body=password, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiGroup General
 * @apiVersion 0.4.0
 * @apiSuccess {String} version Version of the Ict
 * @apiSuccess {String} update Version of the latest Ict version. This field is returned only if a version is available that's newer than the value of the `version` field.
 * @apiSuccess {Object} default_config Default configuration settings. **Not the custom configuration of the node.**
 * @apiSuccessExample {json} 200 Success
 * {
 * "default_config": {
 *     "anti_spam_abs": 1000,
 *     "gui_enabled": true,
 *     "gui_password": "change_me_now",
 *     "gui_port": 2187,
 *     "host": "0.0.0.0",
 *     "max_forward_delay": 200,
 *     "max_heap_size": 1.01,
 *     "min_forward_delay": 0,
 *     "name": "ict",
 *     "neighbors": [],
 *     "port": 1337,
 *     "round_duration": 60000,
 *     "tangle_capacity": 10000
 *     },
 * "success": true,
 * "version": "0.5"
 * }
 */
class RouteGetInfo extends RouteImpl {

    protected RouteGetInfo(JsonIct jsonIct) { super(jsonIct, "/getInfo"); }

    @Override
    public JSONObject execute(Request request) {
        return jsonIct.getInfo();
    }
}

/**
 * @api {post} /update/ update
 * @apiParam {String} version Version of the Ict to download.
 * @apiDescription Download the given Ict version on a node. To check if a newer version of the Ict is available, use the [`getInfo`](#getInfo) endpoint.
 * @apiName update
 * @apiExample {curl} Curl
 * curl http://localhost:2187/update \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&version=0.5'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&version=0.5";
 *                
 * var options = {
 * url: 'http://localhost:2187/update',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&version=0.5'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/update', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiGroup General
 * @apiVersion 0.4.0
 * @apiUse basicSuccess
 */
class RouteUpdate extends RouteImpl {

    protected RouteUpdate(JsonIct jsonIct) { super(jsonIct, "/update"); }

    @Override
    public JSONObject execute(Request request) throws IOException {
        String version = request.queryParams("version");
        return jsonIct.update(version);
    }
}
/**
 * @api {post} /getConfig/ getConfig
 * @apiDescription Get a node's Ict configuration settings.
 * @apiName getConfig
 * @apiGroup Config
 * @apiExample {curl} Curl
 * curl http://localhost:2187/getConfig \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var password = "password=change_me_now";
 *                
 * var options = {
 * url: 'http://localhost:2187/getConfig',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: password
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * password = 'password=change_me_now'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/getConfig', body=password, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiSuccess {Object} config Ict configuration settings for the node
 * @apiSuccessExample {json} 200 Success
 * {
 *  "anti_spam_abs": 1000,
 *  "gui_enabled": true,
 *  "gui_password": "",
 *  "gui_port": 2187,
 *  "host": "0.0.0.0",
 *  "max_forward_delay": 200,
 *  "max_heap_size": 1.01,
 *  "min_forward_delay": 0,
 *  "name": "ict",
 *  "neighbors": [
 *   "URL:PORT",
 *   "URL:PORT",
 *   "URL:PORT"
 *  ],
 *  "port": 1337,
 *  "round_duration": 60000,
 *  "success": true,
 *  "tangle_capacity": 10000
 * }
 */
class RouteGetConfig extends RouteImpl {

    protected RouteGetConfig(JsonIct jsonIct) { super(jsonIct, "/getConfig"); }

    @Override
    public JSONObject execute(Request request) {
        return jsonIct.getConfig();
    }
}

/**
 * @api {post} /setConfig/ setConfig
 * @apiParam {Object} config Ict configuration settings to update for the node. The `config` object must be in the same structure as the one returned from the [`getConfig`](#getConfig) endpoint and include all fields.
 * @apiDescription Update a node's Ict configuration settings. The new configuration settings are stored in the ict.cfg file.
 * Depending on the exact changes, the node might be restarted to apply the changes.
 * @apiName setConfig
 * @apiGroup Config
 * @apiExample {curl} Curl
 * curl http://localhost:2187/setConfig \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&config={"anti_spam_abs": 1000,
 * "gui_enabled": true,"gui_password": "change_me_now",
 * "gui_port": 2187,"host": "0.0.0.0","max_forward_delay": 200,
 * "max_heap_size": 1.01,"min_forward_delay": 0,"name": "ict",
 * "neighbors": ["URL:PORT","URL:PORT","URL:PORT"],"port": 1337,
 * "round_duration": 60000,"tangle_capacity": 1000}'
 * @apiExample {js} NodeJS
 * var request = require('request');
 * 
 * var config = {
 *   anti_spam_abs: 1000,
 *   gui_enabled: true,
 *   gui_password: "change_me_now",
 *   gui_port: 2187,
 *   host: "0.0.0.0",
 *   max_forward_delay: 200,
 *   max_heap_size: 1.01,
 *   min_forward_delay: 0,
 *   name: "ict",
 *   neighbors: ["URL:PORT","URL:PORT","URL:PORT"],
 *   port: 1337,
 *   round_duration: 60000,
 *   tangle_capacity: 1000
 *   };
 * 
 * var data = "password=change_me_now&config=" + JSON.stringify(config);
 * 
 * var options = {
 *   url: 'http://localhost:2187/setConfig',
 *   method: 'POST',
 *   headers: {
 *     'Content-Type': 'application/x-www-form-urlencoded'
 *   },
 *   form: data
 * };
 * 
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *     var result = JSON.parse(data);
 *     console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 *
 * config = {
 *  'anti_spam_abs': 1000,
 *  'gui_enabled': True,
 *  'gui_password': 'change_me_now',
 *  'gui_port': 2187,
 *  'host': '0.0.0.0',
 *  'max_forward_delay': 200,
 *  'max_heap_size': 1.01,
 *  'min_forward_delay': 0,
 *  'name': 'ict',
 *  'neighbors': ('URL:PORT', 'URL:PORT', 'URL:PORT'),
 *  'port': 1337,
 *  'round_duration': 60000,
 *  'tangle_capacity': 1000
 * }
 *
 * data = 'password=change_me_now&config={}'.format(json.dumps(config))
 *
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 *
 * http = urllib3.PoolManager()
 * response = http.request('POST', 'http://localhost:2187/setConfig', body=data, headers=headers)
 * results = json.loads(response.data.decode('utf-8'))
 *
 * print(json.dumps(results, indent=2, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiUse basicSuccess
 */
class RouteSetConfig extends RouteImpl {

    protected RouteSetConfig(JsonIct jsonIct) { super(jsonIct, "/setConfig"); }

    public JSONObject execute(Request request) {
        String configString = request.queryParams("config");
        return jsonIct.setConfig(configString);
    }
}

/**
 * @api {post} /getNeighbors/ getNeighbors
 * @apiDescription Get all neighbors that a node is connected to and the statistics for their most recent communications.
 * @apiName getNeighbors
 * @apiGroup Neighbors
 * @apiExample {curl} Curl
 * curl http://localhost:2187/getNeighbors \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var password = "password=change_me_now";
 *                
 * var options = {
 * url: 'http://localhost:2187/getNeighbors',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: password
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * password = 'password=change_me_now'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/getNeighbors', body=password, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiSuccess {Array} neighbors All neighbors that the node is connected to
 * @apiSuccessExample {json} 200 Success
 * {
 *  "neighbors": [
 *             {
 *              "address": "http://example.com:1337",
 *              "stats": [
 *                     {
 *                      "timestamp": 1547437313, "all": 192, "new": 76, "requested": 3, "invalid": 0, "ignored": 5},
 *                     ...
 *                 ]
*              },
 *             ...
 *         ]
 *  "success": true
 * }
 */
class RouteGetNeighbors extends RouteImpl {

    protected RouteGetNeighbors(JsonIct jsonIct) { super(jsonIct, "/getNeighbors"); }

    public JSONObject execute(Request request) {
        return new JSONObject().put("neighbors", jsonIct.getNeighbors());
    }
}

/**
 * @api {post} /addNeighbor/ addNeighbor
 * @apiUse neighborParam
 * @apiDescription Add a given neighbor to a node.
 * @apiName addNeighbor
 * @apiGroup Neighbors
 * @apiExample {curl} Curl
 * curl http://localhost:2187/addNeighbor \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&address=example.com:1337'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&address=example.com:1337";
 *                
 * var options = {
 * url: 'http://localhost:2187/addNeighbor',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&address=example.com:1337'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/addNeighbor', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiUse basicSuccess
 * @apiErrorExample {json} Format Error
 * {
 * "success": false,
 * "error": "Address does not match required format 'host:port'."
 * }
 * @apiErrorExample {json} TooManyNeighbors Error
 * {"success": false,
 * "error": "Already reached maximum amount of neighbors."
 * }
 */
class RouteAddNeighbor extends RouteImpl {

    protected RouteAddNeighbor(JsonIct jsonIct) { super(jsonIct, "/addNeighbor"); }

    public JSONObject execute(Request request) {
        String address = request.queryParams("address");
        return jsonIct.addNeighbor(address);
    }
}

/**
 * @api {post} /removeNeighbor/ removeNeighbor
 * @apiUse neighborParam
 * @apiDescription Removes a neighbor from the current Ict instance.
 * @apiName RemoveNeighbor
 * @apiGroup Neighbors
 * @apiExample {curl} Curl
 * curl http://localhost:2187/removeNeighbor \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&address=example.com:1337'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&address=example.com:1337";
 *                
 * var options = {
 * url: 'http://localhost:2187/removeNeighbor',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&address=example.com:1337'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/removeNeighbor', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiUse basicSuccess
 * @apiErrorExample {json} NeighborNotFound Error
 * {
 * "success": false,
 * "error": "No neighbor with address 'example.com:1337'."
 * }
 */
class RouteRemoveNeighbor extends RouteImpl {

    protected RouteRemoveNeighbor(JsonIct jsonIct) { super(jsonIct, "/removeNeighbor"); }

    public JSONObject execute(Request request) {
        String address = request.queryParams("address");
        return jsonIct.removeNeighbor(address);
    }
}

/**
 * @api {post} /addModule/ addModule
 * @apiParam {String} user_slash_repo Path to a precompiled .jar file on GitHub in the format `username/repository` (for example, `iotaledger/chat.ixi`)
 * @apiDescription Install an IXI module by downloading the precompiled .jar file from GitHub.
 * @apiName addModule
 * @apiGroup Modules
 * @apiExample {curl} Curl
 * curl http://localhost:2187/addModule \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&user_slash_repo=iotaledger/chat.ixi'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&user_slash_repo=iotaledger/chat.ixi";
 *                
 * var options = {
 * url: 'http://localhost:2187/addModule',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&user_slash_repo=iotaledger/chat.ixi'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/addModule', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiUse basicSuccess
 * @apiErrorExample {json} NoReleasesFound Error
 * {
 * "success": false, "error": "No releases in repository iotaledger/chat.ixi"
 * }
 * @apiErrorExample {json} NoAssetsFound Error
 * {
 * "success": false,
 * "error": "No assets found in release '1.0'"
 * }
 */
class RouteAddModule extends RouteImpl {

    protected RouteAddModule(JsonIct jsonIct) { super(jsonIct, "/addModule"); }

    public JSONObject execute(Request request) {
        String repository = request.queryParams("user_slash_repo");
        return jsonIct.addModule(repository);
    }
}

/**
 * @api {post} /removeModule/ removeModule
 * @apiUse modulePathParam
 * @apiDescription Deletes an installed IXI module from a node.  To check which modules are installed on a node, use the [GetModules](#getModules) endpoint.
 * @apiName removeModule
 * @apiGroup Modules
 * @apiExample {curl} Curl
 * curl http://localhost:2187/removeModule \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&path=chat.ixi-1.4.jar'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&path=chat.ixi-1.4.jar";
 *                
 * var options = {
 * url: 'http://localhost:2187/removeModule',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&path=chat.ixi-1.4.jar'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/removeModule', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiUse basicSuccess
 * @apiUse moduleNotFoundErrorExample
 */
class RouteRemoveModule extends RouteImpl {

    protected RouteRemoveModule(JsonIct jsonIct) { super(jsonIct, "/removeModule"); }

    public JSONObject execute(Request request) {
        String path = request.queryParams("path");
        return jsonIct.removeModule(path);
    }
}

/**
 * @api {post} /updateModule/ updateModule
 * @apiUse modulePathParam
 * @apiParam {String} version Version of the IXI module to install
 * @apiDescription Delete a module and install a different version of it. To get the latest version of a module, use the [GetModules](#getModules) endpoint.
 * @apiName updateModule
 * @apiGroup Modules
 * @apiExample {curl} Curl
 * curl http://localhost:2187/updateModule \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&path=chat.ixi-1.4.jar&version=1.4'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&path=chat.ixi-1.4.jar&version=1.4";
 *                
 * var options = {
 * url: 'http://localhost:2187/updateModule',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&path=chat.ixi-1.4.jar&version=1.4'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/updateModule', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiUse basicSuccess
 * @apiUse moduleNotFoundErrorExample
 */
class RouteUpdateModule extends RouteImpl {

    protected RouteUpdateModule(JsonIct jsonIct) { super(jsonIct, "/updateModule"); }

    public JSONObject execute(Request request) throws Throwable {
        String path = request.queryParams("path");
        String version = request.queryParams("version");
        return jsonIct.updateModule(path, version);
    }
}

/**
 * @api {post} /getModules/ getModules
 * @apiDescription Get all IXI modules that are installed on a node.
 * @apiName getModules
 * @apiGroup Modules
 * @apiExample {curl} Curl
 * curl http://localhost:2187/getModules \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var password = "password=change_me_now";
 *                
 * var options = {
 * url: 'http://localhost:2187/getModules',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: password
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * password = 'password=change_me_now'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/getModules', body=password, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.4.0
 * @apiSuccess {Array} modules All the IXI modules that are installed on the node. The `update` field is returned only if a newer version is available. If the `configurable` field is set to `true`, the
 * module can be configured. To configure a module, use the [`getModuleConfig`](#getModuleConfig) endpoint.
 * @apiSuccessExample {json} 200 Success
 *     {
 *         "modules": [
 *             {
 *                 "path": "chat.ixi.jar",
 *                 "name": "CHAT.ixi",
 *                 "description": "...",
 *                 ...
 *                 "update": "1.3",
 *                 "configurable": true
*              },
 *             ...
 *         ],
 *         "success": true
 *     }
 * @apiUse moduleNotFoundErrorExample
 */

class RouteGetModules extends RouteImpl {

    protected RouteGetModules(JsonIct jsonIct) { super(jsonIct, "/getModules"); }

    public JSONObject execute(Request request) {
        return new JSONObject().put("modules", jsonIct.getModules());
    }
}

/**
 * @api {post} /getModuleConfig/ getModuleConfig
 * @apiUse modulePathParam
 * @apiDescription Get the current and the default configuration settings of an IXI module that's installed on a node.
 * @apiName getModuleConfig
 * @apiGroup Modules
 * @apiExample {curl} Curl
 * curl http://localhost:2187/getModuleConfig \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&path=chat.ixi-1.4.jar'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&path=chat.ixi-1.4.jar";
 *                
 * var options = {
 * url: 'http://localhost:2187/getModuleConfig',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&path=chat.ixi-1.4.jar'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/getModuleConfig', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.5.0
 * @apiSuccess {Object} config The current configuration settings of the IXI module.
 * @apiSuccess {Object} default_config The default configuration settings of the IXI module. Use this object if you want to reset the configuration settings.
 * @apiSuccessExample {json} 200 Success
 * {
 *  "config": { "color": "red", ... },
 *  "default_config": { "color": "blue", ... },
 *  "success": true
 * }
 * @apiUse moduleNotFoundErrorExample
 */
class RouteGetModuleConfig extends RouteImpl {

    protected RouteGetModuleConfig(JsonIct jsonIct) { super(jsonIct, "/getModuleConfig"); }

    public JSONObject execute(Request request) {
        String path = request.queryParams("path");
        return jsonIct.getModuleConfig(path);
    }
}

/**
 * @api {post} /setModuleConfig/ setModuleConfig
 * @apiUse modulePathParam
 * @apiParam {Object} config The new configuration object. The `config` object must be in the same structure as the one returned from the [`getModuleConfig`](#getModuleConfig) endpoint and include all fields.
 * @apiDescription Change the configuration settings of a given IXI module that's installed on a node. The new configuration settings are stored in the module's .cfg file.
 * @apiName SetModuleConfig
 * @apiGroup Modules
 * @apiExample {curl} Curl
 * curl http://localhost:2187/setModuleConfig \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&path=chat.ixi-1.4.jar&config={"password":"Bk6ZxLxu7ANvcCKmoI3O",
 * "channels":["casual","ict","announcements","speculation"],
 * "contacts":["VSVSXLQW"],"username":"Anonymous"}
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&path=chat.ixi-1.4.jar&config={"password":"Bk6ZxLxu7ANvcCKmoI3O",
 * "channels":["casual","ict","announcements","speculation"],
 * "contacts":["VSVSXLQW"],"username":"Anonymous"}";
 *                
 * var options = {
 * url: 'http://localhost:2187/setModuleConfig',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&path=chat.ixi-1.4.jar&config={"password":"Bk6ZxLxu7ANvcCKmoI3O",
 * "channels":["casual","ict","announcements","speculation"],
 * "contacts":["VSVSXLQW"],"username":"Anonymous"}'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/setModuleConfig', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiVersion 0.5.0
 * @apiUse basicSuccess
 * @apiUse moduleNotFoundErrorExample
 * @apiErrorExample {json} ModuleNotFound Error
 * {
 * "success": false,
 * "error": "Property 'color' cannot be assigned to value 'helicopter'."
 * }
 */
class RouteSetModuleConfig extends RouteImpl {

    protected RouteSetModuleConfig(JsonIct jsonIct) { super(jsonIct, "/setModuleConfig"); }

    public JSONObject execute(Request request) {
        String path = request.queryParams("path");
        JSONObject newConfig = new JSONObject(request.queryParams("config"));
        return jsonIct.setModuleConfig(path, newConfig);
    }
}

/**
 * @api {post} /getLogs/ getLogs
 * @apiDescription Get all log messages within a given index interval.
 * @apiParam {Number} [min] Index of first message that you want to read
 * @apiParam {Number} [max] Index of last message that you want to read
 * @apiSuccess {Boolean} block If `true`, the log message with the index of the `min` parameter is not yet available. The log message will be returned when it's available.
 * @apiName getLogs
 * @apiGroup Log
 * @apiVersion 0.5.0
 * @apiExample {curl} Curl
 * curl http://localhost:2187/getLogs \
 * -X POST \
 * -H 'Content-Type: application/x-www-form-urlencoded' \
 * -d 'password=change_me_now&min=0&max=20'
 * @apiExample {js} NodeJS
 * var request = require('request');
 *
 * var data = "password=change_me_now&min=0&max=20";
 *                
 * var options = {
 * url: 'http://localhost:2187/getLogs',
 * method: 'POST',
 * headers: {
 * 'Content-Type': 'application/x-www-form-urlencoded'
 * },
 * form: data
 * };
 *
 * request(options, function (error, response, data) {
 *   if (!error && response.statusCode == 200) {
 *       var result = JSON.parse(data);
 *       console.log(JSON.stringify(result, null, 1));
 *   }
 * });
 * @apiExample {python} Python
 * import json
 * import urllib3
 * 
 * data = 'password=change_me_now&min=0&max=20'
 * 
 * headers = {
 *    'content-type': 'application/x-www-form-urlencoded'
 * }
 * 
 * http = urllib3.PoolManager()
 * 
 * response = http.request('POST', 'http://localhost:2187/getLogs', body=data, headers=headers)
 * 
 * results = json.loads(response.data.decode('utf-8'))
 * 
 * print(json.dumps(results, indent=1, sort_keys=True))
 * @apiSuccess {Array} logs Array (limited length) of logs in ascending index order.
 * @apiSuccess {Number} min Index of first available log message to read.
 * @apiSuccess {Number} max Index of last available log message to read.
 * @apiSuccessExample {json} 200 Success
 * {
 *  "logs": [
 *        {
 *         "level": "info",
 *         "timestamp": 1547437313,
 *         "message": "Sender/Neighbor]   102  |90   |0    |0    |0       localhost/127.0.0.1:14265"
 *        },
 *        {
 *         "level": "warn",
 *         "timestamp": 1547439294,
 *         "message": "[Receiver/Ict]   Received invalid transaction from neighbor: localhost/127.0.0.1:14265 (issuance timestamp not in tolerated interval)"
 *        },
 *             ...
 *        ],
 *  "min": 0,
 *  "max": 112,
 *  "success": true
 * }
 */
class RouteGetLogs extends RouteImpl {

    protected RouteGetLogs(JsonIct jsonIct) { super(jsonIct, "/getLogs"); }

    public JSONObject execute(Request request) throws InterruptedException {
        int min = Integer.parseInt(request.queryParamOrDefault("min", ""+LogAppender.getIndexMin()));
        boolean block = Boolean.parseBoolean(request.queryParamOrDefault("block", "false"));

        synchronized (LogAppender.NOTIFY_SYNCHRONIZER) {
            while (block && min > LogAppender.getIndexMax()) {
                LogAppender.NOTIFY_SYNCHRONIZER.wait();
            }
        }

        int max = Math.min(Integer.parseInt(request.queryParamOrDefault("max", ""+LogAppender.getIndexMax())), min+1000);

        JSONArray logs = new JSONArray();
        for(int i = min; i <= max; i++) {
            LogEvent event = LogAppender.getLogEvent(i);
            if(event != null)
                logs.put(logEventToJSON(i, event));
        }

        return new JSONObject().put("logs", logs).put("min", LogAppender.getIndexMin()).put("max", LogAppender.getIndexMax());
    }

    protected static JSONObject logEventToJSON(int index, LogEvent event) {
        return new JSONObject().put("index", index).put("message", event.getMessage().getFormattedMessage()).put("timestamp", event.getTimeMillis()).put("level", event.getLevel());
    }
}
