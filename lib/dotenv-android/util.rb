# frozen_string_literal: true

module DotEnvAndroid
  class Util
    def self.to_snakecase(string)
      string.gsub(/(.)([A-Z])/, '\1_\2').downcase
    end

    def self.snake_to_camel(string)
      # takes API_HOST => ApiHost
      s = string.split('_').collect(&:capitalize).join
      # Takes ApiHost => apiHost
      s[0] = s[0].downcase
      s
    end
  end
end
