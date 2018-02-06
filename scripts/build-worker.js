#!/usr/bin/env node

var fs = require('fs-extra');
var child = require('child_process');
var platform = process.argv[2];
var deviceType = process.argv[3];
var rn = JSON.parse(fs.readFileSync('.re-natal'));

function resolveIosDevHost(deviceType) {
    if (deviceType === 'simulator') {
        return 'localhost';
    } else {
        return child
            .execSync('ipconfig getifaddr en0', {stdio: ['pipe', 'pipe', 'ignore']})
            .toString()
            .trim()
    }
}

function resolveAndroidDevHost(deviceType) {
    allowedTypes = {
        'real': 'localhost',
        'avd': '10.0.2.2',
        'genymotion': '10.0.3.2'
    };

    return allowedTypes[deviceType];
}

function resolveDevHost(platform, deviceType) {
    if(platform === 'android') {
        return resolveAndroidDevHost(deviceType);
    } else {
        return resolveIosDevHost(deviceType);
    }
}

function createIndexFile(platform, deviceType) {
    var index = "var modules={};\n";
    var modules = rn["worker-modules"];
    for (var i = 0, len = modules.length; i < len; i++) {
        var module = modules[i];
        index += `modules['${module}']=require('${module}');\n`;
    }

    index += `var devHost = '${resolveDevHost(platform, deviceType)}';\n`;
    index += `require('worker-bridge').withModules(modules).loadApp(devHost);\n`;

    return index;
}

fs.writeFile(
    "worker.thread.js",
    createIndexFile(platform, deviceType),
    function (err){
        if(err) {
            return console.log(err);
        }

        console.log("worker.thread.js created!");
    });


