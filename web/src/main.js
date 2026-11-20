import { createApp, ref, onMounted } from 'vue'
import axios from 'axios'

const App = {
  setup() {
    const jobs = ref([])
    const loadJobs = async () => {
      const res = await axios.get('/api/jobs')
      jobs.value = res.data
    }
    onMounted(loadJobs)
    return { jobs }
  },
  template: `
    <h2>Job 管理中心</h2>
    <ul>
      <li v-for="job in jobs" :key="job.id">{{ job.jobName }} - {{ job.status }}</li>
    </ul>
  `
}

createApp(App).mount('#app')
