import http from './http';

export const hotelsApi = {
  getHotelDetail(id) {
    return http.get(`/hotels/${id}`);
  },
  // add more hotel-related endpoints here
};

export default hotelsApi;
