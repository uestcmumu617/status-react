#!/bin/sh

# rn-nodeify
# temporary hack due to https://github.com/facebook/react-native/issues/4968
./node_modules/.bin/rn-nodeify --install "assert,zlib,buffer,inherits,console,constants,crypto,dns,domain,events,http,https,os,path,process,punycode,querystring,fs,stream,string_decoder,timers,tty,url,util,net,vm" --hack;
sed -i "" -e "s/_bridge = bridge;$/_bridge = bridge;if\(\!\[\[bridge.bundleURL absoluteString\] containsString:@\"index\"\]\) return;/g" node_modules/realm/react-native/ios/RealmReact/RealmReact.mm
