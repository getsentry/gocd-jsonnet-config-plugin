{
  "pipelines": {
    "pipe1": {
      "group": "group",
      "materials": {
        "upstream": {
          "pipeline": "upstream-pipeline-1",
          "stage": "test1"
        }
      },
      "stages": [
        {
          "build-image": {
            "clean_workspace": true,
            "jobs": {
              "build-image": {
                "artifacts": [
                  {
                    "build": {
                      "source": "image_ref"
                    }
                  }
                ],
                "tasks": [
                  {
                    "exec": {
                      "command": "bash",
                      "arguments": [`
                        "-c",
                        "docker build"
                      ]
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