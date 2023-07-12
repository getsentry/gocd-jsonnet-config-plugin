local tasks = import "github.com/getsentry/gocd-jsonnet/v1.0.0/gocd-tasks.libsonnet";
{
  "pipelines": {
    "pipe1": {
      "group": "simple" + tasks.script("test"),
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
