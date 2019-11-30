require 'date'
require File.expand_path('../lib/dotenv-android/version', __FILE__)

Gem::Specification.new do |s|
  s.name          = 'dotenv-android'
  s.version       = DotEnvAndroid::Version.get
  s.date          = Date.today.to_s
  s.summary       = 'Access environment variables at runtime of Android app'
  s.description   = 'It is said to be good practice when you configure your app with a .env file. This library will (1) scan your source code looking for requests for environment variables, (2) parse a .env file and the variables defined on the machine, and (3) generate source code file to compile into app.' 
  s.authors       = ['Levi Bostian']
  s.email         = 'levi.bostian@gmail.com'
  s.files         = Dir.glob('{bin,lib}/**/*') + %w[LICENSE README.md CHANGELOG.md]
  s.bindir        = "bin"
  s.executables   = ["dotenv-android"]
  s.require_paths = ["lib"]
  s.homepage      = 'http://github.com/levibostian/dotenv-android'
  s.license       = 'MIT'
  s.add_runtime_dependency 'colorize', '~> 0.8', '>= 0.8.1'
  s.add_runtime_dependency 'dotenv', '~> 2.7', '>= 2.7.5'  
  s.add_development_dependency 'rubocop', '~> 0.58', '>= 0.58.2'
  s.add_development_dependency 'rake', '~> 12.3', '>= 12.3.1'
  s.add_development_dependency 'rspec', '~> 3.8', '>= 3.8.0'
  s.add_development_dependency 'rspec_junit_formatter', '~> 0.4', '>= 0.4.1'
end