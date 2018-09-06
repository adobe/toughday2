# Deploying to S3
This project is able to deploy the artifact to S3 using the [maven-s3-wagon](https://github.com/jcaddel/maven-s3-wagon) 
plugin. In order to use and configure maven for S3 deployment one must get hold of AWS access and secret keys that have 
appropriate permission on a S3 bucket. 

## Configuration
In order to deploy to S3 one must configure it's maven settings with the S3 bucket name and the AWS access key and AWS 
secret key. Steps to perform:

### Encrypt the AWS secret key
It's recommended to use [Maven Password Encryption](https://maven.apache.org/guides/mini/guide-encryption.html) to
encrypt the AWS secret key (e.g. NpT45lGrl8rjVtPDGWEkAXITxU56P1Hxz) for use in the maven settings.xml file
(like with passwords).

```
$ mvn -ep NpT45lGrl8rjVtPDGWEkAXITxU56P1Hxz
{RHvm+kpJpFAHwMNgOh/293R6l+nr1luTs4X8RmD/PyenjBfXGHedZ8YkhARCO0oCNb/2XyTaujpoFb6mpEUs4A==}
```

### Modify the settings.xml
Modify your _~/.m2/settings.xml_ file. Add a new `<server>` element. Use your AWS access key for the `<username>` and
the encrypted AWS secret for the `<password>`.

```
<servers>
...
    <server>
        <id>s3-deploy-repo</id>
        <username>AKICIIQBZF123DIIL2BKHQ</username>
        <password>{RHvm+kpJpFAHwMNgOh/293R6l+nr1luTs4X8RmD/PyenjBfXGHedZ8YkhARCO0oCNb/2XyTaujpoFb6mpEUs4A==}</password>
        <filePermissions>Private</filePermissions>
    </server>
...
</servers>

```

## Deploying to S3
Once the configuration is done, one can deploy to S3 using the `deploy:deploy-file` goal:
```
mvn deploy:deploy-file -Durl=s3://<bucket-name>/big-bear/binaries/toughday -DrepositoryId=s3-deploy-repo -Dfile=target/toughday2-0.2.2-SNAPSHOT.jar -DpomFile=pom.xml
```

## Deploying a release to S3
AFAIK maven can only deal with one deployment repo at the same time, so it's not possible deploy during the release to
Artifactory and S3 in one go. The workaround is to first run a regular release that deploys to Artifactory, then 
checkout the release tag and run the deploy to S3.

Checkout the release tag (adjust to the release tag chosen during release:prepare step):
```
git checkout -b toughday2-0.1.0 tags/toughday2-0.1.0
```

Build:
```
mvn clean install
```

Deploy the tag to S3, adjust S3 url accordingly:
```
mvn deploy:deploy-file -Durl=s3://<bucket-name>/big-bear/binaries/toughday -DrepositoryId=s3-deploy-repo -Dfile=target/toughday2-0.1.0.jar -DpomFile=pom.xml
```