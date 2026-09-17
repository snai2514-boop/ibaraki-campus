#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/dist
python3 tools/generate_project.py
node --test Tests/core.test.cjs
for test in Tests/test-school-*.cjs; do node "$test"; done
xcodebuild -project CampusAssistant.xcodeproj -scheme CampusAssistant -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' -derivedDataPath build/device CODE_SIGNING_ALLOWED=NO build > build/device-build.log 2>&1 || { tail -100 build/device-build.log; exit 1; }
mkdir -p build/package/Payload
cp -R build/device/Build/Products/Release-iphoneos/CampusAssistant.app build/package/Payload/
(cd build/package && zip -qry ../dist/CampusAssistant-1.0.80-unsigned.ipa Payload)
xcodebuild -project CampusAssistant.xcodeproj -scheme CampusAssistant -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath build/simulator CODE_SIGNING_ALLOWED=NO build > build/simulator-build.log 2>&1 || { tail -100 build/simulator-build.log; exit 1; }
ditto -c -k --sequesterRsrc --keepParent build/simulator/Build/Products/Debug-iphonesimulator/CampusAssistant.app build/dist/CampusAssistant-Simulator.zip
shasum -a 256 build/dist/* > build/dist/SHA256SUMS.txt
echo 'Device IPA (unsigned) and simulator app built. IPA requires personal Apple signing before installation.'
