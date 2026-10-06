import type { Config } from 'jest';

const config: Config = {
  moduleFileExtensions : ['js', 'json', 'ts'],
  rootDir              : 'src',
  testRegex            : '.*\\.spec\\.ts$',
  transform            : { '^.+\\.(t|j)s$': 'ts-jest' },
  collectCoverageFrom  : [
    '**/*.(t|j)s',
    '!**/*.module.(t|j)s',
    '!**/main.(t|j)s',
    '!**/*.dto.(t|j)s',
    '!**/*.entity.(t|j)s',
    '!**/migrations/**',
  ],
  coverageDirectory    : '../coverage',
  testEnvironment      : 'node',
  moduleNameMapper     : {
    '^src/(.*)$': '<rootDir>/$1',
  },
  coverageThreshold    : {
    global: {
      branches  : 70,
      functions : 75,
      lines     : 80,
      statements: 80,
    },
  },
};

export default config;
