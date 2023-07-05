{
  "pipelines": {
    "pipe1": {
      "group": "simple",
      "materials": [
        {
          "mygit": {
            "git": "http://my.example.org/mygit.git"
          }
        }
      ],
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
