import React from 'react';
import { Grid } from '@mui/material';
import MediaControlCard from './MediaControlCard';

export default function CategoryView() {
  return (
    <Grid container spacing={{ xs: 2, md: 3 }} columns={{ xs: 4, sm: 8, md: 12 }}>
      {Array.from(Array(6)).map((_, index) => (
        <Grid item xs={2} sm={4} md={4} key={index}>
          <MediaControlCard />
        </Grid>
      ))}
    </Grid>
  );
}
