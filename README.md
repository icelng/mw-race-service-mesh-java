主要还是用netty框架来做的。踩了一个巨坑————没有使用http的Keep-Alive，而使得consumer的cpu使用率狂飙。回想起来，踩到这个坑并且还踩了这么久，真觉得很傻逼。
