// // import http from './http';

// // export default {
// //   getById: async (userId) => {
// //     const res = await http.get(`/users/${userId}`);
// //     return res.data;
// //   },
// // };
// import http from './http';

// export default {
//   // JWT 토큰 헤더 포함
//   getInfo: async () => {
//     const token = localStorage.getItem('token');
//     const res = await http.get('/user/info', {
//       headers: { Authorization: `Bearer ${token}` }
//     });
//     return res.data;
//   },
// };

import http from './http';

export default {
  // JWT 토큰 헤더 포함 사용자 정보 조회
  getInfo: async () => {
    const token = localStorage.getItem('token');
    const res = await http.get('/user/info', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return res.data;
  },

  // 특정 사용자 ID로 조회
  getById: async (userId) => {
    const res = await http.get(`/users/${userId}`);
    return res.data;
  },
};
