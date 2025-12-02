/**
 * 节点配置
 * 根据用户院系映射到对应的后端节点
 */

export const NODE_MAP = {
  '计算机系': 'http://localhost:58080/api',
  '软件工程系': 'http://localhost:58082/api',
  '信息学院': 'http://localhost:58083/api',
  'default': 'http://localhost:58080/api'
}

/**
 * 根据院系获取节点 URL
 * @param {string} department - 院系名称
 * @returns {string} 节点 URL
 */
export function getNodeUrl(department) {
  return NODE_MAP[department] || NODE_MAP['default']
}
