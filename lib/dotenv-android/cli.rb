# frozen_string_literal: true

require 'colorize'
require 'optparse'
require 'set'
require 'pathname'
require_relative './ui'
require_relative './util'
require_relative './generator'
require_relative './version'
require 'dotenv'

module DotEnvAndroid
  Options = Struct.new(:source, :out, :verbose, :debug, :package_name)

  class CLI
    def initialize
      @options = parse_options

      @ui = DotEnvAndroid::UI.new(@options.verbose, @options.debug)

      assert_options

      DotEnvAndroid::Generator.new(@options).start
    end

    def parse_options
      options = Options.new
      options.verbose = false
      options.debug = false

      opt_parser = OptionParser.new do |opts|
        opts.banner = 'Usage: dotenv-android [options]'

        opts.on('-v', '--version', 'Print version') do
          puts DotEnvAndroid::Version.get
          exit
        end
        opts.on('-s', '--source DIR', 'Source code directory to check for requested environment variables') do |source|
          options.source = source
          options.out = Pathname.new(source).join('Env.kt') # set default
        end
        opts.on('--verbose', 'Verbose output') do
          options.verbose = true
        end
        opts.on('--debug', 'Debug output (also turns on verbose)') do
          options.verbose = true
          options.debug = true
        end
        opts.on('--package PACKAGE_NAME', 'Package name to add to the top of the generated Env.kt file (example: com.yourdomain.app, or PACKAGE_NAME environment variable found in .env)') do |package_name|
          options.package_name = package_name
        end
        opts.on('-o', '--out FILE', 'Output file (example: Path/Env.kt)') do |out|
          options.out = out
        end
        opts.on('-h', '--help', 'Prints this help') do
          puts opts
          exit
        end
      end

      help = opt_parser.help
      abort(help) if ARGV.empty?

      opt_parser.parse!(ARGV)

      options
    end

    def assert_options
      prefix = '[ERROR]'
      @ui.fail("#{prefix} --source required") unless @options.source
      @ui.fail("#{prefix} --package required") unless @options.package_name
    end
  end
end
