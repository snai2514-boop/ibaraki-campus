"""Deterministic Xcode project. Standard library only; no external build generator."""
from pathlib import Path
import hashlib
root=Path(__file__).resolve().parents[1]
objects={}
def uid(s):return hashlib.sha256(s.encode()).hexdigest()[:24].upper()
def add(name,value):objects[uid(name)]=value;return uid(name)
def arr(values):return '('+','.join(values)+',)' if values else '()'
def configs(name,settings):
    ids=[]
    for mode in ['Debug','Release']:
        config=dict(settings)
        if mode=='Debug':config.update(SWIFT_OPTIMIZATION_LEVEL='"-Onone"',DEBUG_INFORMATION_FORMAT='dwarf')
        else:config.update(SWIFT_COMPILATION_MODE='wholemodule',SWIFT_OPTIMIZATION_LEVEL='"-O"',DEBUG_INFORMATION_FORMAT='"dwarf-with-dsym"')
        ids.append(add(name+mode,'{isa=XCBuildConfiguration; buildSettings={'+''.join(k+'='+v+';' for k,v in config.items())+'}; name='+mode+';}'))
    return add(name+'configs','{isa=XCConfigurationList; buildConfigurations='+arr(ids)+'; defaultConfigurationIsVisible=0; defaultConfigurationName=Release;}')
def file(path,kind):return add(path,'{isa=PBXFileReference; lastKnownFileType='+kind+'; path="'+path+'"; sourceTree="<group>";}')
def build(path,ref):return add('build'+path,'{isa=PBXBuildFile; fileRef='+ref+';}')
swift=file('CampusAssistant/AppDelegate.swift','sourcecode.swift')
web=file('CampusAssistant/Web','folder')
school=file('CampusAssistant/SchoolScripts','folder')
privacy=file('CampusAssistant/PrivacyInfo.xcprivacy','text.xml')
icon=file('CampusAssistant/Assets.xcassets','folder.assetcatalog')
uitest=file('Tests/CampusUITests.swift','sourcecode.swift')
app=add('product','{isa=PBXFileReference; explicitFileType=wrapper.application; path=CampusAssistant.app; sourceTree=BUILT_PRODUCTS_DIR;}')
testproduct=add('testproduct','{isa=PBXFileReference; explicitFileType=wrapper.cfbundle; path=CampusUITests.xctest; sourceTree=BUILT_PRODUCTS_DIR;}')
products=add('products','{isa=PBXGroup; children='+arr([app,testproduct])+'; name=Products; sourceTree="<group>";}')
group=add('main','{isa=PBXGroup; children='+arr([swift,web,school,privacy,icon,uitest,products])+'; sourceTree="<group>";}')
def phase(name,kind,files):return add(name,'{isa='+kind+'; buildActionMask=2147483647; files='+arr(files)+'; runOnlyForDeploymentPostprocessing=0;}')
sources=phase('sources','PBXSourcesBuildPhase',[build('swift',swift)])
resources=phase('resources','PBXResourcesBuildPhase',[build('web',web),build('school',school),build('privacy',privacy),build('icon',icon)])
frameworks=phase('frameworks','PBXFrameworksBuildPhase',[])
common=dict(SDKROOT='iphoneos',IPHONEOS_DEPLOYMENT_TARGET='16.0',SWIFT_VERSION='5.0',TARGETED_DEVICE_FAMILY='"1,2"',CODE_SIGN_STYLE='Automatic',CLANG_ENABLE_MODULES='YES',CLANG_ENABLE_OBJC_ARC='YES',ENABLE_USER_SCRIPT_SANDBOXING='YES')
appconfigs=configs('app',dict(common,PRODUCT_NAME='"$(TARGET_NAME)"',PRODUCT_BUNDLE_IDENTIFIER='com.campusassistant.ibaraki.ios',INFOPLIST_FILE='CampusAssistant/Info.plist',ASSETCATALOG_COMPILER_APPICON_NAME='AppIcon'))
target=add('target','{isa=PBXNativeTarget; buildConfigurationList='+appconfigs+'; buildPhases='+arr([sources,frameworks,resources])+'; buildRules=(); dependencies=(); name=CampusAssistant; productName=CampusAssistant; productReference='+app+'; productType="com.apple.product-type.application";}')
proxy=add('proxy','{isa=PBXContainerItemProxy; containerPortal='+uid('project')+'; proxyType=1; remoteGlobalIDString='+target+'; remoteInfo=CampusAssistant;}')
dep=add('dep','{isa=PBXTargetDependency; target='+target+'; targetProxy='+proxy+';}')
testconfigs=configs('test',dict(common,PRODUCT_NAME='"$(TARGET_NAME)"',PRODUCT_BUNDLE_IDENTIFIER='com.campusassistant.ibaraki.ios.uitests',GENERATE_INFOPLIST_FILE='YES',TEST_TARGET_NAME='CampusAssistant'))
testtarget=add('testtarget','{isa=PBXNativeTarget; buildConfigurationList='+testconfigs+'; buildPhases='+arr([phase('testsources','PBXSourcesBuildPhase',[build('uitest',uitest)]),phase('testframeworks','PBXFrameworksBuildPhase',[])])+'; buildRules=(); dependencies='+arr([dep])+'; name=CampusUITests; productName=CampusUITests; productReference='+testproduct+'; productType="com.apple.product-type.bundle.ui-testing";}')
project=add('project','{isa=PBXProject; attributes={LastUpgradeCheck=1600;}; buildConfigurationList='+configs('project',common)+'; compatibilityVersion="Xcode 14.0"; developmentRegion=zh-Hans; hasScannedForEncodings=0; knownRegions=(en,"zh-Hans",Base); mainGroup='+group+'; productRefGroup='+products+'; projectDirPath=""; projectRoot=""; targets='+arr([target,testtarget])+';}')
folder=root/'CampusAssistant.xcodeproj';folder.mkdir(exist_ok=True)
(folder/'project.pbxproj').write_text('// !$*UTF8*$!\n{archiveVersion=1;classes={};objectVersion=56;objects={\n'+''.join(k+'='+v+';\n' for k,v in objects.items())+'};rootObject='+project+';}\n',encoding='utf-8')
schemes=folder/'xcshareddata/xcschemes';schemes.mkdir(parents=True,exist_ok=True)
def ref(id,name):return f'<BuildableReference BuildableIdentifier="primary" BlueprintIdentifier="{id}" BuildableName="{name}" BlueprintName="{name.split(".")[0]}" ReferencedContainer="container:CampusAssistant.xcodeproj"/>'
(schemes/'CampusAssistant.xcscheme').write_text(f'''<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion="1600" version="1.3"><BuildAction parallelizeBuildables="YES" buildImplicitDependencies="YES"><BuildActionEntries><BuildActionEntry buildForTesting="YES" buildForRunning="YES" buildForProfiling="YES" buildForArchiving="YES" buildForAnalyzing="YES">{ref(target,'CampusAssistant.app')}</BuildActionEntry></BuildActionEntries></BuildAction><TestAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.IDEFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv="YES"><Testables><TestableReference skipped="NO">{ref(testtarget,'CampusUITests.xctest')}</TestableReference></Testables></TestAction><LaunchAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.IDEFoundation.Launcher.LLDB" launchStyle="0" useCustomWorkingDirectory="NO" ignoresPersistentStateOnLaunch="NO" debugDocumentVersioning="YES" allowLocationSimulation="YES"><BuildableProductRunnable runnableDebuggingMode="0">{ref(target,'CampusAssistant.app')}</BuildableProductRunnable></LaunchAction><ProfileAction buildConfiguration="Release" shouldUseLaunchSchemeArgsEnv="YES" savedToolIdentifier="" useCustomWorkingDirectory="NO" debugDocumentVersioning="YES"><BuildableProductRunnable runnableDebuggingMode="0">{ref(target,'CampusAssistant.app')}</BuildableProductRunnable></ProfileAction><AnalyzeAction buildConfiguration="Debug"/><ArchiveAction buildConfiguration="Release" revealArchiveInOrganizer="YES"/></Scheme>''',encoding='utf-8')
print('Generated Xcode project and shared scheme')
