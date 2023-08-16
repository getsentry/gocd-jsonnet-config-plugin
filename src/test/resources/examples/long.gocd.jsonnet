local pipeline_names = ['pipe0', 'pipe1', 'pipe2', 'pipe3', 'pipe4', 'pipe5', 'pipe6', 'pipe7', 'pipe8', 'pipe9', 'pipe10', 'pipe11', 'pipe12'];
local stages = ['build1', 'build2', 'build3', 'build4', 'build5', 'build6', 'build7', 'build8', 'build9', 'build10', 'build11', 'build12', 'build13', 'build14', 'build15'];

{
  pipelines: {
    [name]: {
      group: 'simple',
      materials: { mygit: { git: 'http://my.example.org/mygit.git' } },
      stages: [
        { [stage]: { jobs: { build: { tasks: [{ exec: { command: 'make' } }] } } } },
        for stage in stages
      ],
    }
    for name in pipeline_names
  },
}
