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

- Maintenance functions from music.pl
- Ability to combine groups to filter tracks
- Build Containers
- Deploy Containers on CRC Openshift


## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.
