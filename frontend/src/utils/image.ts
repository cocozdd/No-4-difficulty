export const getFallbackImageByCategory = (category?: string) => {
  switch (category) {
    case '电子产品':
    case 'Electronics':
      return 'https://dummyimage.com/600x360/1e90ff/ffffff.png&text=Electronics';
    case '书籍资料':
    case 'Books':
      return 'https://dummyimage.com/600x360/34d399/ffffff.png&text=Books';
    case '生活用品':
    case 'Daily':
    default:
      return 'https://dummyimage.com/600x360/f97316/ffffff.png&text=Daily';
  }
};
