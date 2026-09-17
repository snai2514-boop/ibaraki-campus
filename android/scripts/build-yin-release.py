"""Build a locally signed YIN release; never print or archive private credentials."""
from pathlib import Path
import os, json, secrets, subprocess
root=Path(__file__).resolve().parents[1]
private=root/'.private'; private.mkdir(exist_ok=True)
config=private/'release-signing.json'
jdk=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'))
if not config.exists():
    data={'password':secrets.token_urlsafe(36),'alias':'yin-ibaraki','keystore':str(private/'yin-ibaraki.jks')}
    env=os.environ.copy(); env['YIN_KEY_PASSWORD']=data['password']
    subprocess.run([str(jdk/'bin/keytool.exe'),'-genkeypair','-keystore',data['keystore'],'-storetype','PKCS12',
        '-storepass:env','YIN_KEY_PASSWORD','-keypass:env','YIN_KEY_PASSWORD','-alias',data['alias'],
        '-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=YIN, OU=Ibaraki Campus Assistant, O=YIN'],env=env,check=True,capture_output=True)
    config.write_text(json.dumps(data),encoding='utf-8')
data=json.loads(config.read_text(encoding='utf-8'))
env=os.environ.copy(); env.update(JAVA_HOME=str(jdk),GRADLE_USER_HOME=str(root/'.gradle-user'),
    RELEASE_SIGNING_CONFIG_FILE=data['keystore'],RELEASE_STORE_PASSWORD=data['password'],
    RELEASE_KEY_ALIAS=data['alias'],RELEASE_KEY_PASSWORD=data['password'])
with (root/'build/yin-release-build.log').open('w',encoding='utf-8') as log:
    result=subprocess.run([str(root/'gradlew.bat'),'testDebugUnitTest','lintRelease','assembleRelease','-Pkotlin.compiler.execution.strategy=in-process','--console=plain'],cwd=root,env=env,stdout=log,stderr=subprocess.STDOUT)
print('Release build '+('succeeded' if result.returncode==0 else 'failed; see build/yin-release-build.log'))
raise SystemExit(result.returncode)
