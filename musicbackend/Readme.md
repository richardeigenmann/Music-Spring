# Music Backend

This is a Spring Boot application built with Kotlin and Gradle.

## Running the application

You can run the application using the Gradle wrapper included in the project.

```bash
./gradlew bootRun
```

The application will start on port 8002 by default.
## CORS Configuration

When you deploy, you can override this property without changing the code. 
For example, if you deploy your Angular app to https://www.your-music-app.com, 
you would set an environment variable for your Spring Boot container:

`APP_CORS_ALLOWED-ORIGINS=https://www.your-music-app.com`

### Supporting Multiple Origins

Yes, multiple origins can be comma-separated. The way the `CorsConfig.kt` is set up, Spring Boot will automatically handle a comma-separated list for you.
When you define the property in `application.properties` like this: 
`app.cors.allowed-origins=http://localhost:4200,https://your-app.com,https://staging.your-app.com`

The `@Value` annotation injects this into the `allowedOrigins: Array<String>`, and Spring automatically splits the comma-separated string into an array of individual origins. The `allowedOrigins(*allowedOrigins)` call then correctly registers each one.

## Database

By default, the application uses an H2 in-memory database. 

**Security Warning:** In production or public-facing deployments, the H2 console should be disabled to prevent unauthorized database access.

### Disabling H2 Console
To disable the H2 console, set the following property in your `application.properties` or as an environment variable:

`SPRING_H2_CONSOLE_ENABLED=false`

In the provided `application.properties`, this is currently set to `false` by default.

## Starting the container

```bash
docker run -p 8002:8002 \
  -v /richi/mp3:/richi/mp3 \
  -v /richi/ToDo:/richi/ToDo \
  -d musicbackend:0.0.1-SNAPSHOT
  
  
# to check on permission problems
docker run --rm -it \
  -u 1002:1000 \
  -v /richi/mp3:/richi/mp3 \
  -v /richi/ToDo:/richi/ToDo \
  busybox sh

docker tag musicbackend:0.1.0-SNAPSHOT richardeigenmann/musicbackend:0.1.0-SNAPSHOT
docker login
docker push richardeigenmann/musicbackend:0.1.0-SNAPSHOT

docker run -p 8002:8002 \
  -u 1002:1000 \
  -v /richi/mp3:/richi/mp3 \
  -v /richi/ToDo:/richi/ToDo \
  -d richardeigenmann/musicbackend:0.1.0-SNAPSHOT

crc console --credentials
crc oc-env
oc login -u developer -p developer https://api.crc.testing:6443 --insecure-skip-tls-verify=true

# Create a new namespace/project
oc new-project music-backend
oc new-project music-backend-v2

# Deploy the image from Docker Hub
oc create service clusterip music-backend --tcp=8002:8002
oc new-app --name=music-backend --image=docker.io/richardeigenmann/musicbackend:0.1.0-SNAPSHOT

oc set volume deployment/music-backend --add \
    --name=music-storage \
    --type=pvc \
    --claim-name=music-data-pvc \
    --mount-path=/richi
oc set env deployment/music-backend \
    APP_CORS_ALLOWED_ORIGINS="http://music-frontend-default.apps-crc.testing"


oc expose deployment/music-backend --port=8002 --target-port=8002
oc expose svc/music-backend --hostname=music-backend-v2.apps-crc.testing

curl music-backend-default.apps-crc.testing:8002/api/version
curl music-backend-default.apps-crc.testing/api/version
curl music-backend-default.apps-crc.testing:8002/api/totalTrackCount
curl music-backend-v2.apps-crc.testing/api/version

# Update to new version:
oc set image deployment/music-backend music-backend=docker.io/richardeigenmann/music-backend:0.1.0-SNAPSHOT
# Then, force a fresh pull just in case:
oc patch deployment music-backend -p '{"spec":{"template":{"metadata":{"annotations":{"update-date":"'$(date +%s)'"}}}}}'

## Maintenance Endpoints

- `POST /api/clear-db`: Clears all music data tables (TrackGroup, TrackFile, Track, Groups) while preserving GroupTypes.
curl music-backend-default.apps-crc.testing:8002/api/totalTrackCount

# Describe Volume Mounts
oc describe deployment music-app | grep -A 10 Volumes

# Need to do this ad admin
crc console --credentials

oc label ns music-backend pod-security.kubernetes.io/enforce=privileged

oc create -f - <<EOF
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: music-data-pvc
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 50Gi
EOF

oc set volume deployment/music-app --add \
  --name=music-data-volume \
  --type=pvc \
  --claim-name=music-data-pvc \
  --mount-path=/richi



# spin up a container with the PVC attached and use that to do the rsync
oc run rsync-helper --image=registry.access.redhat.com/ubi9/ubi-minimal --overrides='
{
  "spec": {
    "containers": [
      {
        "name": "helper",
        "image": "registry.access.redhat.com/ubi9/ubi-minimal",
        "command": ["/bin/sh", "-c", "microdnf install -y rsync && sleep 3600"],
        "volumeMounts": [{"name": "richi", "mountPath": "/richi"}]
      }
    ],
    "volumes": [{"name": "richi", "persistentVolumeClaim": {"claimName": "music-data-pvc"}}]
  }
}'

# Then 
oc rsync /richi/mp3/ rsync-helper:/richi/mp3/
oc rsync /home/richi/ToDo/ rsync-helper:/richi/ToDo/

oc exec rsync-helper -- du -sh /richi
oc exec rsync-helper -- ls /richi/mp3
oc delete pod rsync-helper --force --grace-period=0


# Now hit
# http://music-app-music-backend.apps-crc.testing:8002


```
