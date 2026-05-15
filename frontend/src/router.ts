import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import Register from './views/Register.vue'
import Layout from './views/Layout.vue'
import HomePage from './views/HomePage.vue'
import TagsPage from './views/TagsPage.vue'
import TopicsPage from './views/TopicsPage.vue'
import SearchPage from './views/SearchPage.vue'
import AiChatPage from './views/AiChatPage.vue'
import ManagePage from './views/ManagePage.vue'
import ArticleDetail from './views/ArticleDetail.vue'
import TopicDetailPage from './views/TopicDetailPage.vue'
import AdminLayout from './views/admin/AdminLayout.vue'
import AdminDashboard from './views/admin/AdminDashboard.vue'
import AdminUsers from './views/admin/AdminUsers.vue'
import AdminDocuments from './views/admin/AdminDocuments.vue'
import AdminTags from './views/admin/AdminTags.vue'
import AdminTopics from './views/admin/AdminTopics.vue'
import AdminStats from './views/admin/AdminStats.vue'
import { isAdmin } from './utils/auth'
import { getAccessToken } from './api'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login,
        meta: { requiresAuth: false }
    },
    {
        path: '/register',
        name: 'Register',
        component: Register,
        meta: { requiresAuth: false }
    },
    {
        path: '/',
        component: Layout,
        children: [
            { path: '', name: 'Home', component: HomePage },
            { path: 'tags', name: 'Tags', component: TagsPage },
            { path: 'topics', name: 'Topics', component: TopicsPage },
            { path: 'search', name: 'Search', component: SearchPage },
            { path: 'article/:id', name: 'ArticleDetail', component: ArticleDetail },
            { path: 'topic/:id', name: 'TopicDetail', component: TopicDetailPage },
            { path: 'ai', name: 'AiChat', component: AiChatPage, meta: { requiresAuth: true } },
            { path: 'manage', name: 'Manage', component: ManagePage, meta: { requiresAuth: true } },
        ]
    },
    {
        path: '/admin',
        component: AdminLayout,
        meta: { requiresAuth: true, requiresAdmin: true },
        children: [
            { path: '', name: 'AdminDashboard', component: AdminDashboard },
            { path: 'users', name: 'AdminUsers', component: AdminUsers },
            { path: 'documents', name: 'AdminDocuments', component: AdminDocuments },
            { path: 'tags', name: 'AdminTags', component: AdminTags },
            { path: 'topics', name: 'AdminTopics', component: AdminTopics },
            { path: 'stats', name: 'AdminStats', component: AdminStats },
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, _from, next) => {
    const token = getAccessToken()

    if (to.meta.requiresAuth && !token) {
        next('/login')
    } else if (to.meta.requiresAdmin && !isAdmin()) {
        next('/')
    } else if ((to.path === '/login' || to.path === '/register') && token) {
        next('/')
    } else {
        next()
    }
})

export default router