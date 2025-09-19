import { createRouter, createWebHistory } from "vue-router";

import Login from "../components/Login.vue";
import Register from "../components/Register.vue";
import Hotels from "../components/Hotels.vue";
import Wishlist from "../components/Wishlist.vue";
import Checkout from "@/components/Checkout.vue";
import PaymentSuccess from "@/components/PaymentSuccess.vue";
import Search from "../components/Search.vue";


const routes = [
  { path: "/", redirect: "/hotels" },
  { path: "/login", component: Login },
  { path: "/register", component: Register },
  { path: "/hotels", component: Hotels },
  { path: "/checkout", component: Checkout },
  { path: "/wishlist", component: Wishlist, meta: { requiresAuth: true } },  
  { path: "/payment/success", component: PaymentSuccess},
  { path: "/search", component: Search }

];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
