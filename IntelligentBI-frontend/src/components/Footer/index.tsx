import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import '@umijs/max';
import React from 'react';
const Footer: React.FC = () => {
  const defaultMessage = '智能 BI 项目';
  const currentYear = new Date().getFullYear();
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      copyright={`${currentYear} ${defaultMessage}`}
      links={[
        {
          key: '智能 BI',
          title: '智能 BI',
          href: 'https://github.com/dujiaqi0725/Intelligent-BI',
          blankTarget: true,
        },
        {
          key: 'github',
          title: <GithubOutlined />,
          href: 'https://github.com/dujiaqi0725/Intelligent-BI',
          blankTarget: true,
        },
        {
          key: '智能 BI',
          title: '智能 BI',
          href: 'https://github.com/dujiaqi0725/Intelligent-BI',
          blankTarget: true,
        },
      ]}
    />
  );
};
export default Footer;
