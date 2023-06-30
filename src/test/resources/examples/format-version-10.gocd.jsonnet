{
  "format_version": 10,
  "pipelines": {
    "pipe1": {
      "group": "simple",
      "materials": {
        "mygit": {
          "git": "http://my.example.org/mygit.git",
          "whitelist": [
            "externals",
            "tools"
          ]
        }
      },
      "stages": [
        {
          "build": {
            "jobs": {
              "build": {
                "tasks": [
                  {
                    "exec": {
                      "command": "make"
                    }
                  }
                ]
              }
            }
          }
        }
      ]
    },
    "pipe2": {
      "group": "simple",
      "materials": {
        "mygit": {
          "git": "http://my.example.org/mygit.git",
          "includes": [
            "externals",
            "tools"
          ]
        }
      },
      "stages": [
        {
          "build": {
            "jobs": {
              "build": {
                "tasks": [
                  {
                    "exec": {
                      "command": "make"
                    }
                  }
                ]
              }
            }
          }
        }
      ]
    }
  }
}
