const { parseVersion } = require('./utils');
const { DefinePlugin } = require("webpack");
const { ModuleFederationPlugin } = require('webpack').container;
const { dependencies } = require('./package.json');
const path = require("path");

const aliases = {
  '@bc/shared': path.join(path.resolve(__dirname, '.'), "node_modules", "@bc", "shared"),
  'styled-components': path.join(path.resolve(__dirname, '.'), "node_modules", "styled-components"),
  'react': path.join(path.resolve(__dirname, '.'), "node_modules", "react"),
  'react-dom': path.join(path.resolve(__dirname, '.'), "node_modules", "react-dom")
};

module.exports = {
  webpack: {
    alias: aliases,
    configure: {
      ...(process.env.NODE_ENV !== 'production'
         ? {
             entry: './test/index.tsx',
           }
         : {
             output: {
               publicPath: '/wm/TestModule/',
             }
           })
    },
    plugins: {
      remove: process.env.NODE_ENV !== 'production'
          ? []
          : [ 'HtmlWebpackPlugin' , 'MiniCssExtractPlugin' ],
      add: [
        new DefinePlugin({
          'process.env.BUILD_TIMESTAMP': `'${new Date().toISOString()}'`,
          'process.env.BUILD_VERSION': `'${parseVersion()}'`,
        }),
        ...(process.env.NODE_ENV !== 'production'
            ? []
            : [
                new ModuleFederationPlugin({
                  name: "TestModule",
                  filename: 'remoteEntry.js',
                  exposes: {
                    List: './src/List',
                    Form: './src/Form',
                  },
                  shared: {
                    react: {
                      singleton: true,
                      requiredVersion: dependencies["react"],
                    },
                    "react-dom": {
                      singleton: true,
                      requiredVersion: dependencies["react-dom"],
                    },
                  },
                }),
              ])
      ]
    }
  },
  plugins: [
    {
      plugin: {
        overrideWebpackConfig: ({ webpackConfig, pluginOptions, context: { paths } }) => {
          const moduleScopePlugin = webpackConfig.resolve.plugins.find(plugin => plugin.appSrcs && plugin.allowedFiles);
          if (moduleScopePlugin) {
            Object
                .keys(aliases)
                .map(key => aliases[key])
                .forEach(path => moduleScopePlugin.appSrcs.push(path));
          }
//          webpackConfig.resolve.extensionAlias = {
//                ".js": [".ts", ".tsx", ".js", ".mjs"],
//                ".mjs": [".mts", ".mjs"]
//              };
          const ignoreWarnings = [
              { module: /@microsoft\/fetch-event-source/ }
            ];
          return { ...webpackConfig, ignoreWarnings }
        }
      }
    }
  ]
};
