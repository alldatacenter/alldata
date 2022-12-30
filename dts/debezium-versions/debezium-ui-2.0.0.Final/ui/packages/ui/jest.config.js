
// For a detailed explanation regarding each configuration property, visit:
// https://jestjs.io/docs/en/configuration.html

module.exports = {
  "modulePaths": [
    "<rootDir>"
  ],
  // Automatically clear mock calls and instances between every test
  clearMocks: true,

  // Indicates whether the coverage information should be collected while executing the test
  collectCoverage: true,

  // The directory where Jest should output its coverage files
  coverageDirectory: 'coverage',

  // An array of directory names to be searched recursively up from the requiring module's location
  moduleDirectories: [
    "node_modules",
    "<rootDir>/src"
  ],

  // A map from regular expressions to module names that allow to stub out resources with a single module
  moduleNameMapper: {
    "\\.(css|less)$":
      "identity-obj-proxy",
    "\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$": "<rootDir>/__mocks__/fileMock.js",
    "^shared(.*)$": "<rootDir>/src/app/shared$1",
    "^components(.*)$": "<rootDir>/src/app/components$1",
    "^assets(.*)$": "<rootDir>/assets$1",
    "^i18n(.*)$": "<rootDir>/src/i18n$1",
    "^layout(.*)$": "<rootDir>/src/app/layout$1",
    // "^@debezium/ui-services(.*)$": "<rootDir>/__mocks__/fileMock.js"
    "^@debezium/ui-services(.*)$": "<rootDir>/../services/src$1",
    "^@debezium/ui-models(.*)$": "<rootDir>/../models/src$1"
  },

  // A preset that is used as a base for Jest's configuration
  preset: "ts-jest/presets/js-with-ts",

  // The path to a module that runs some code to configure or set up the testing framework before each test
  setupFilesAfterEnv: ["<rootDir>/test-setup.ts"],

  // The test environment that will be used for testing.
  testEnvironment: "jsdom",

  // A list of paths to snapshot serializer modules Jest should use for snapshot testing
  snapshotSerializers: ['enzyme-to-json/serializer'],

};
