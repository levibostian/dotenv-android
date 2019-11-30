# frozen_string_literal: true

require 'colorize'
require 'optparse'
require 'set'
require 'pathname'
require_relative './ui'
require_relative './util'
require 'dotenv'

module DotEnvAndroid
  class Generator
    def initialize(options)
      @options = options

      @ui = DotEnvAndroid::UI.new(@options.verbose, @options.debug)

      @all_env_vars = Dotenv.parse('.env')
    end

    def start
      requests = iterate_source
      env_variables = get_values(requests)
      generate_output(env_variables)
    end

    def iterate_source
      source_pattern = File.expand_path("#{@options.source}/**/*.kt")
      @ui.verbose("Searching for environment vars in source: #{source_pattern}")

      requests = Set[]

      Dir.glob(source_pattern) do |kotlin_file|
        next if File.directory? kotlin_file

        @ui.verbose("Looking for Env usage in: #{kotlin_file}")
        requests.merge(get_env_requests(kotlin_file))
        @ui.verbose("Found #{requests.count} requests")
        @ui.debug("Requests found for file: #{requests.to_a}")
      end

      requests.to_a
    end

    def get_env_requests(file)
      requests = []

      File.readlines(file).each do |line|
        line.split(' ').each do |word|
          # https://regexr.com/4pmp0
          next unless /Env\.[a-z]\w*/.match? word

          requested_variable = word.split('.')[1]
          requested_variable = DotEnvAndroid::Util.to_snakecase(requested_variable).upcase

          requests.push(requested_variable)
        end
      end

      requests
    end

    def get_values(requests)      
      values = {}

      requests.each do |request|
        @ui.fail("Environment variable #{request} not found in .env") unless @all_env_vars[request]

        values[request] = @all_env_vars[request]
      end

      @ui.debug("Values: #{values}")
      values
    end

    def get_package_header
      package_name_header = "package #{@options.package_name}"

      if !@options.package_name.include? "."
        package_name = @all_env_vars[@options.package_name]
        @ui.fail("Cannot find package name in .env file with key, #{@options.package_name}") if package_name.nil?
        package_name_header = "package #{package_name}"
      end 

      @ui.debug("Package name header: #{package_name_header}")

      return package_name_header
    end 

    def generate_output(env_variables)
      @ui.verbose("Outputting environment variables to #{@options.out}")

      package_name_header = get_package_header
      file_contents = "#{package_name_header}\n\n"      
      file_contents += "object Env {\n\n"
      env_variables.each do |key, value|
        file_contents += "  val #{DotEnvAndroid::Util.snake_to_camel(key)} = \"#{value}\"\n"
      end

      file_contents += "\n}"

      @ui.debug("Output file: #{file_contents}")

      File.open(@options.out, 'w') { |file| file.write(file_contents) }

      @ui.success('Environment variables file generated!')
      @ui.success("File generated, #{@options.out}. If you don't like this output path, run command again with `-o` CLI option")
    end
  end
end
