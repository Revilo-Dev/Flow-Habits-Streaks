const path = require("path");
const migrationsDir = path.resolve(__dirname, "../../../../uhabits-core/assets/main/migrations");
const testAssetsDir = path.resolve(__dirname, "../../../../uhabits-core/assets/test");
const fontsDir = path.resolve(__dirname, "../../../../uhabits-core/assets/main/fonts");

config.set({
    browserNoActivityTimeout: 120000,
    browserDisconnectTimeout: 30000,
    browserDisconnectTolerance: 3,
    client: {
        mocha: {
            timeout: 30000
        }
    },
    proxies: {
        '/migrations/': '/absolute' + migrationsDir + '/',
        '/test-assets/': '/absolute' + testAssetsDir + '/',
        '/fonts/': '/absolute' + fontsDir + '/'
    }
});

config.files.push(
    { pattern: migrationsDir + '/*.sql', included: false, served: true, watched: false },
    { pattern: testAssetsDir + '/**/*', included: false, served: true, watched: false },
    { pattern: fontsDir + '/*.ttf', included: false, served: true, watched: false }
);

// Use sql-asm.js (pure JS, no WASM binary loading) to avoid karma-webpack
// issue #498 where WASM assets in webpack's temp output dir are not served.
config.webpack.resolve = config.webpack.resolve || {};
config.webpack.resolve.fallback = Object.assign(
    config.webpack.resolve.fallback || {},
    { fs: false, path: false, crypto: false }
);
config.webpack.resolve.alias = Object.assign(
    config.webpack.resolve.alias || {},
    { "sql.js": path.resolve(__dirname, "../../node_modules/sql.js/dist/sql-asm.js") }
);
