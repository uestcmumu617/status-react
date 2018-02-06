/*
 * Originally taken from https://github.com/decker405/figwheel-react-native
 *
 * @providesModule worker-bridge
 */

var debugEnabled = false;

var config = {
    basePath: "target/",
    googBasePath: 'goog/',
    serverPort: 8081
};

var self;
var evaluate = eval; // This is needed, direct calls to eval does not work (RN packager???)
var externalModules = {};
var evalListeners = {};
var asyncImportChain = new Promise(function (succ,fail) {succ(true);});

function fireEvalListenters(url) {
    Object.values(evalListeners).forEach(function (listener) {
        listener(url)
    });
}

function logDebug(msg) {
    if (debugEnabled) {
        console.log(msg);
    }
}

var isChrome = function () {
    return typeof importScripts === "function"
};

function asyncImportScripts(url, success, error) {
    logDebug('(asyncImportScripts) Importing: ' + url);
    asyncImportChain =
	asyncImportChain
	.then(function (v) {return fetch(url);})
        .then(function (response) {
	    if(response.ok) 
		return response.text();
	    throw new Error("Failed to Fetch: " + url + " - Perhaps your project was cleaned and you haven't recompiled?")
        })
        .then(function (responseText) {
	    evaluate(responseText);
	    fireEvalListenters(url);
            success();
	    return true;
        })
        .catch(function (e) {
            console.error(e);
            error();
	    return true;
        });
}

function syncImportScripts(url, success, error) {
    try {
        importScripts(url);
        logDebug('Evaluated: ' + url);
	fireEvalListenters(url);
        success();
    } catch (e) {
        console.error(e);
        error()
    }
}

// Loads js file sync if possible or async.
function importJs(src, success, error) {
    var noop = function(){};
    success = (typeof success == 'function') ? success : noop;
    error   = (typeof error   == 'function') ? error   : noop;
    logDebug('(importJs) Importing: ' + src);
    if (isChrome()) {
        syncImportScripts(src, success, error);
    } else {
        asyncImportScripts(src, success, error);
    }
}

function interceptRequire() {
    var oldRequire = window.require;
    console.info("Shimming require");
    window.require = function (id) {
        console.info("Requiring: " + id);
        if (externalModules[id]) {
            return externalModules[id];
        }
        return oldRequire(id);
    };
}

function serverBaseUrl(host) {
    return "http://" + host + ":" + config.serverPort
}

function loadApp(devHost) {
    var fileBasePath = serverBaseUrl(devHost) + "/" + config.basePath + "worker";


    if (typeof goog === "undefined") {
        console.info('Loading Closure base.');
        interceptRequire();
	// need to know base path here
        importJs(fileBasePath + '/goog/base.js', function () {
            shimBaseGoog(fileBasePath);
            importJs(fileBasePath + '/cljs_deps.js');
            importJs(fileBasePath + '/goog/deps.js', function () {
                // This is needed because of RN packager
                // seriously React packager? why.
                var googreq = goog.require;

                googreq(`status_im.worker`);
            });
        });
    }
}

function withModules(moduleById) {
    externalModules = moduleById;
    return self;
}

// Goog fixes
function shimBaseGoog(basePath) {
    console.info('Shimming goog functions.');
    goog.basePath = basePath + '/' + config.googBasePath;
    goog.writeScriptSrcNode = importJs;
    goog.writeScriptTag_ = function (src, optSourceText) {
        importJs(src);
        return true;
    };
}

self = {
    withModules: withModules,
    loadApp: loadApp
};

module.exports = self;
