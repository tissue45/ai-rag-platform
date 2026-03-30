<script setup lang="ts">
type Props = {
  maxWidth?: number
}

const props = withDefaults(defineProps<Props>(), {
  maxWidth: 980,
})
</script>

<template>
  <div class="container" :style="{ maxWidth: `${props.maxWidth}px` }">
    <slot />
  </div>
</template>

<style scoped>
.container {
  width: 100%;
  margin: 0 auto;
  padding: 0 20px;
  box-sizing: border-box;
}
</style>

