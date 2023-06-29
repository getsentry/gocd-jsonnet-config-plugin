{
  "pipelines": {
    "pipe2": {
      "group": "rich",
      "label_template": "${mygit[:8]}",
      "locking": "on",
      "tracking_tool": {
        "link": "http://your-trackingtool/yourproject/${ID}",
        "regex": "evo-(\\d+)"
      },
      "timer": {
        "spec": "0 0 22 ? * MON-FRI",
        "only_on_changes": true
      },
      "materials": {
        "mygit": {
          "git": "http://my.example.org/mygit.git",
          "branch": "ci"
        },
        "upstream": {
          "type": "dependency",
          "pipeline": "pipe2",
          "stage": "test"
        }
      },
      "stages": [
        {
          "build": {
            "clean_workspace": true,
            "approval": {
              "type": "manual",
              "allow_only_on_success": true,
              "roles": [
                "manager"
              ]
            },
            "jobs": {
              "csharp": {
                "run_instances": 3,
                "resources": [
                  "net45"
                ],
                "artifacts": [
                  {
                    "build": {
                      "source": "bin/",
                      "destination": "build"
                    }
                  },
                  {
                    "test": {
                      "source": "tests/",
                      "destination": "test-reports/"
                    }
                  }
                ],
                "tabs": {
                  "report": "test-reports/index.html"
                },
                "environment_variables": {
                  "MONO_PATH": "/usr/bin/local/mono"
                },
                "secure_variables": {
                  "PASSWORD": "s&Du#@$xsSa"
                },
                "properties": {
                  "perf": {
                    "source": "test.xml",
                    "xpath": "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"
                  }
                },
                "tasks": [
                  {
                    "fetch": {
                      "pipeline": "pipe2",
                      "stage": "build",
                      "job": "test",
                      "source": "test-bin/",
                      "destination": "bin/"
                    }
                  },
                  {
                    "exec": {
                      "command": "make",
                      "arguments": [
                        "VERBOSE=true"
                      ]
                    }
                  },
                  {
                    "script": "./build.sh ci"
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
