# Music Frontend

This project was generated with [Angular CLI](https://github.com/angular/angular-cli).

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.
## Who is this repo for?

This repo currently is of use to it's developer only.

## Setting up on a new computer

Prerequisites: You need to have NodeJs and Angular 21 installed

```bash
su -
npm install -g @angular/cli
npm install -g angular-cli-ghpages

git clone https://github.com/richardeigenmann/<<add repo here>>
cd Music-Spring/musicfrontend
tree -I 'node_modules|coverage|dist' # to list the folder structure
```


## How to run the unit tests

Run `ng test` to execute the unit tests.

## Running end-to-end tests

```bash
ng serve -o # ensure that the application is running on localhost:4200
npx cypress open
```

Then click on "E2E Testing", pick a browser and "Start E2E Testing".
Then look for the spec.cy.js hypelink and click on it. The tests should run.

## Upgrading

```bash
npm outdated
ng update
npm update
npx npm-check-updates -u
npm run updateBuildTimeStamp
ng test
npx cypress open
```

## Linting

```bash
ng lint
```

## Functionality to Add

- New Tracks should be added to a new tracks playlist
- Maintenance functions from music.pl
- Zap DB endpoint

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Container
npm run container:build
docker tag musicfrontend richardeigenmann/musicfrontend:0.0.1
docker login
docker push richardeigenmann/musicfrontend:0.0.1
docker tag musicfrontend richardeigenmann/musicfrontend:latest
docker push richardeigenmann/musicfrontend:latest

./gradlew :musicfrontend:pushDocker

 To build and push everything, you can now simply run:
   1 ./gradlew buildDocker  # For frontend
   2 ./gradlew :musicbackend:bootBuildImage # For backend (if using Spring Boot's builder)
  Or use the pushDocker task I added for the frontend. This structure makes the entire deployment process much more predictable and easier to automate.



oc expose svc/music-frontend --port=80

oc get svc music-frontend

# pull a fresh copy
oc patch deployment music-frontend -p '{"spec":{"template":{"spec":{"containers":[{"name":"music-frontend","imagePullPolicy":"Always"}]}}}}'

oc new-app --docker-image=docker.io/richardeigenmann/musicfrontend:0.0.1 \
    --name=music-frontend \
    -e BACKEND_URL=http://music-backend.default.svc.cluster.local:8002/

http://music-frontend-default.apps-crc.testing/

docker compose up -d
