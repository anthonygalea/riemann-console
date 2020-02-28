# Riemann Console [![CI](https://github.com/anthonygalea/riemann-console/workflows/CI/badge.svg)](https://github.com/anthonygalea/riemann-console/actions?query=workflow%3ACI+branch%3Amaster)

A dashboard for [Riemann](https://riemann.io)

![Demo](docs/demo.gif)

## Compile

```
git clone git@github.com:anthonygalea/riemann-console.git
cd riemann-console
npm install
lein with-profile prod uberjar
```

Compiles the ClojureScript code then creates a standalone jar.

## Run

```
java -jar target/riemann-console.jar
```

## Configure

All configuration is stored inside an edn file which looks like this:
```clojure
{:port 5557
 :default-dashboard-name "Riemann Console"
 :default-endpoint "127.0.0.1:5556"
 :dashboards {}}
```
By default this file is created at `./riemann-console.edn` if it is missing.
The path can be overridden using the environment variable
`RIEMANN_CONSOLE_CONFIG`.

* `:port` the port the server uses
* `:default-dashboard-name` the name used when a dashboard is created
* `:default-endpoint` the endpoint used when a dashboard is created
* `:dashboards` all configuration for the dashboards you create

## Contribute

1. Suggestions welcome in the
[issue tracker](https://github.com/anthonygalea/riemann-console/issues)
2. For code contributions see [CONTRIBUTING.md](CONTRIBUTING.md)

## Develop

First start the backend:
```
lein run
```

Then in a separate terminal:
```
lein dev
```

Once you see `[:app] Build completed` browse to
[http://localhost:8280](http://localhost:8280)
