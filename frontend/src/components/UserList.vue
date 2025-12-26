<script setup>
import { ref, onMounted } from 'vue'

const users = ref([])

const fetchUsers = async () => {
    try {
        const response = await fetch('/api/system/user/list')
        const data = await response.json()
        users.value = data
    } catch (error) {
        console.error('Error fetching users:', error)
    }
}

onMounted(() => {
    fetchUsers()
})
</script>

<template>
    <div class="user-list">
        <h1>User List</h1>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Username</th>
                    <th>Nickname</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="user in users" :key="user.userId">
                    <td>{{ user.userId }}</td>
                    <td>{{ user.username }}</td>
                    <td>{{ user.nickname }}</td>
                    <td>{{ user.status === 0 ? 'Normal' : 'Disabled' }}</td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<style scoped>
.user-list {
    padding: 20px;
}

table {
    width: 100%;
    border-collapse: collapse;
}

th,
td {
    border: 1px solid #ddd;
    padding: 8px;
    text-align: left;
}

th {
    background-color: #f4f4f4;
}
</style>
