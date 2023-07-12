local utils = import "github.com/getsentry/test-jsonnet/libs/utils.libsonnet";
{
  "pipelines": {
    "pipe1": {
      "group": utils.identity("simple"),
      "materials": {
        "mygit": {
          "git": "http://my.example.org/mygit.git"
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
  },
}
