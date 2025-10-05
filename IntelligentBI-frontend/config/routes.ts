export default [
  { path: '/User', layout: false, routes: [{ path: '/User/login', component: './User/login' }] },
  { path: '/User', layout: false, routes: [{ path: '/User/register', component: './User/register' }] },
  {path:'/',redirect: '/add_chart'},
  {path: '/add_chart',name: "智能分析" , icon: 'BarChartOutlined', component: './AddChart'},
  {path: '/my_chart',name: "我的图表" , icon: 'PieChartOutlined', component: './MyChart'},
  {
    path: '/admin',
    icon: 'crown',
    access: 'canAdmin',
    routes: [
      { path: '/admin', name: '管理页面', redirect: '/admin/sub-page' },
      { path: '/admin/sub-page', name: '管理页面2', component: './Admin' },
    ],
  },
  { path: '/', redirect: '/welcome' },
  { path: '*', layout: false, component: './404' },
];
