# 关于 ConfigureNode

利用这个轮子获得的都是来自`lucko/helper`的`ConfigureNode`对象，所以对其进行一些使用上的总结：

`JSON`、`YAML` 和 `HOCON` 配置文件结构是树形的，每一层都是`ConfigureNode`，Node 有 key 和 value 和 parent 三个基本属性。

key 是 Object 类型(虽然我并不明白为什么 key 会有非 String 的情况)

value 有四种：scalar(向量，就是 int、float、string、boolean 和 class(还不会用)这样的单个的值)、map、list 和 null

- 有 null 就说明一个事情：Node 可以是空的，可以获取一个不存在的路径下的 Node，其值为 null

parent 是父节点，根节点的 parent 为 null

还有 ConfigureOptions 和 attach 属性不知道是干啥的，好像和更高级的序列化/反序列化有关，以后再研究。

## ConfigurationNode 的特点(使用方法)

- `ConfigurationNode.getPath(path...)` 层层路径获得一个 Node，按上面所说，如果这个路径不存在，那么也会返回 node，但是其 value 是 null 类型的，getValue()将返回 null。可以用此作为路径不存在的判断。

- `ConfigurationNode.getValue()`获得节点的值，同时做一定的转换：如果是 List/Map 就会把这个子树下面都做 scalar->objet, map->map, list->list) -> 是不是也是 Immutable？没有测试过

- `ConfigurationNode.getList()`也同理，但是注意获取的是 Immutable，如果要写，需要转换成 Mutable 的。还有一个提供转换方法参数的可能会比较有用。

- `ConfigurationNode.getInt()`等等的，就是把 Scalar 转换成对应的格式。

- `gethindrenList` 和 `getChildrenMap` 获得的依然是 Node 的集合，在遍历的时候比较有用。
