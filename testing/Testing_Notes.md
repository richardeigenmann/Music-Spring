# Testing Notes

docker pull grafana/k6

When using the `k6` docker image, you can't just give the script name since
the script file will not be available to the container as it runs. Instead
you must tell k6 to read `stdin` by passing the file name as `-`. Then you
pipe the actual file into the container with `<` or equivalent. This will
cause the file to be redirected into the container and be read by k6.

docker run --rm -i --network host grafana/k6 run - < testing/first-test.js

docker run --rm -i \
  --cap-add=SYS_ADMIN \
  --security-opt seccomp=unconfined \
  --network host \
  --shm-size=2gb \
  -v $(pwd):/home/k6 \
  -e K6_BROWSER_ENABLED=true \
  grafana/k6:latest-with-browser \
  run - < testing/browser-test.js


k6 run testing/browser-test.js
