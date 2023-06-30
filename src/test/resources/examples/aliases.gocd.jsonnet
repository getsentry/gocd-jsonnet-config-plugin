{
  "common": {
    "scalar_alias": "hello world",
    "task_list_alias": [
      {
        "exec": {
          "command": "init"
        }
      },
      {
        "exec": {
          "command": "make",
          "arguments": [
            "VERBOSE=true"
          ]
        }
      }
    ]
  },
  "pipelines": {
    "pipe1": {
      "group": "aliases",
      "materials": {
        "mygit": {
          "git": "http://my.example.org/mygit.git",
          "branch": "ci"
        }
      },
      "stages": [
        {
          "prepare": {
            "jobs": {
              "prepare": {
                "tasks": [
                  {
                    "exec": {
                      "command": "prepare",
                      "arguments": [
                        "hello world"
                      ]
                    }
                  }
                ]
              }
            }
          }
        },
        {
          "build": {
            "jobs": {
              "build": {
                "tasks": [
                  {
                    "exec": {
                      "command": "init"
                    }
                  },
                  {
                    "exec": {
                      "command": "make",
                      "arguments": [
                        "VERBOSE=true"
                      ]
                    }
                  }
                ]
              }
            }
          }
        },
        {
          "test": {
            "jobs": {
              "test": {
                "tasks": [
                  [
                    {
                      "exec": {
                        "command": "init"
                      }
                    },
                    {
                      "exec": {
                        "command": "make",
                        "arguments": [
                          "VERBOSE=true"
                        ]
                      }
                    }
                  ],
                  {
                    "exec": {
                      "command": "test_unit"
                    }
                  },
                  {
                    "exec": {
                      "command": "test_integration"
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
