# RS2 File Server

A stand-alone server for distributing rs2 cache files.

## Quick guide

Download the latest [released jar](../../releases/) and [file-server.properties](./file-server.properties) into the same
directory.

Update `file-server.properties` with your own client properties.

### Properties

#### Required

| Key |
|----|
| port |
| revision |
| rsaModulus |
| rsaPrivate |

#### Optional

| Key | Default value |
|---|---|
| threads | 0 |
| acknowledgeId | 3 |
| statusId | 0 |
| cachePath | "./cache/" |
| prefetchKeys | - |

> You can remove the `prefetchKeys` property to auto generate them on start-up.